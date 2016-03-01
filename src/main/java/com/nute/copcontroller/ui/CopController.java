package com.nute.copcontroller.ui;

import static com.nute.copcontroller.models.StaticUtils.readMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.MouseInputListener;

import org.jxmapviewer.JXMapViewer;
import org.jxmapviewer.OSMTileFactoryInfo;
import org.jxmapviewer.VirtualEarthTileFactoryInfo;
import org.jxmapviewer.input.CenterMapListener;
import org.jxmapviewer.input.PanKeyListener;
import org.jxmapviewer.input.PanMouseInputListener;
import org.jxmapviewer.input.ZoomMouseWheelListenerCursor;
import org.jxmapviewer.viewer.DefaultTileFactory;
import org.jxmapviewer.viewer.DefaultWaypoint;
import org.jxmapviewer.viewer.GeoPosition;
import org.jxmapviewer.viewer.TileFactory;
import org.jxmapviewer.viewer.Waypoint;
import org.jxmapviewer.viewer.WaypointPainter;
import org.jxmapviewer.viewer.WaypointRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nute.copcontroller.entities.GPSLocation;
import com.nute.copcontroller.entities.Traffic;
import com.nute.copcontroller.entities.WaypointCaught;
import com.nute.copcontroller.entities.WaypointGangster;
import com.nute.copcontroller.entities.WaypointPolice;
import com.nute.copcontroller.models.CopControllerException;

public class CopController extends JFrame {
	private static final long serialVersionUID = 1L;
	private static final Logger LOGGER = LoggerFactory.getLogger(CopController.class);

	private WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<Waypoint>();
	private JXMapViewer jxMapViewer = new JXMapViewer();
	private Map<Long, GPSLocation> lmap;
	private Scanner scanner;
	private String hostname = "localhost";
	private Integer port = 10007;

	SwingWorker<Void, Traffic> worker = new SwingWorker<Void, Traffic>() {

		@Override
		protected Void doInBackground() throws Exception {
			try (Socket trafficServer = new Socket(hostname, port)) {

				OutputStream os = trafficServer.getOutputStream();
				DataOutputStream dos = new DataOutputStream(os);

				dos.writeUTF("<disp>");
				InputStream is = trafficServer.getInputStream();

				scanner = new Scanner(is);

				for (;;) {
					Set<Waypoint> waypoints = new HashSet<Waypoint>();

					Integer time;
					Integer minutes;
					Integer size;

					time = scanner.nextInt();
					minutes = scanner.nextInt();
					size = scanner.nextInt();

					Long ref_from = 0L;
					Long ref_to = 0L;
					Integer step = 0;
					Integer maxstep = 1;
					Integer type = 0;
					Double lat, lon;
					Double lat2, lon2;
					Integer num_captured_gangsters;
					String name = "Cop";

					Map<String, Integer> cops = new HashMap<>();

					for (int i = 0; i < size; ++i) {

						ref_from = scanner.nextLong();
						ref_to = scanner.nextLong();
						maxstep = scanner.nextInt();
						step = scanner.nextInt();
						type = scanner.nextInt();

						if (type == 1) {
							num_captured_gangsters = scanner.nextInt();
							name = scanner.next();

							if (cops.containsKey(name)) {
								cops.put(name, cops.get(name) + num_captured_gangsters);
							} else {
								cops.put(name, num_captured_gangsters);
							}
						}

						lat = lmap.get(ref_from).getLatitude();
						lon = lmap.get(ref_from).getLongitude();

						lat2 = lmap.get(ref_to).getLatitude();
						lon2 = lmap.get(ref_to).getLongitude();

						if (maxstep == 0) {
							maxstep = 1;
						}

						lat += step * ((lat2 - lat) / maxstep);
						lon += step * ((lon2 - lon) / maxstep);

						if (type == 1) {
							waypoints.add(new WaypointPolice(lat, lon, name));
						} else if (type == 2) {
							waypoints.add(new WaypointGangster(lat, lon));
						} else if (type == 3) {
							waypoints.add(new WaypointCaught(lat, lon));
						} else {
							waypoints.add(new DefaultWaypoint(lat, lon));
						}

					}

					if (time >= minutes * 60 * 1000 / 200) {
						scanner = null;
					}

					StringBuilder sb = new StringBuilder();

					int sec = time / 5;
					int min = sec / 60;
					sec = sec - min * 60;
					time = time - min * 60 * 5 - sec * 5;

					sb.append("|");
					sb.append(min);
					sb.append(":");
					sb.append(sec);
					sb.append(":");
					sb.append(2 * time);
					sb.append("|");
					// sb.append(" Justine - Car Window (log player for Robocar City Emulator, Robocar World Championshin in Debrecen)");
					sb.append(Arrays.toString(cops.entrySet().toArray()));

					publish(new Traffic(waypoints, sb.toString()));
				}
			} catch (java.io.IOException e) {

				System.out.println(e.toString());

				CopController.this.dispatchEvent(new WindowEvent(CopController.this, WindowEvent.WINDOW_CLOSING));
			}

			return null;
		}

		@Override
		protected void process(java.util.List<Traffic> traffics) {

			Traffic traffic = traffics.get(traffics.size() - 1);
			setTitle(traffic.getTitle());
			waypointPainter.setWaypoints(traffic.getWaypoints());

			jxMapViewer.repaint();
			repaint();

		}

		@Override
		protected void done() {
		}
	};

	Action paintTimer = new AbstractAction() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 4393548462493846658L;

		@Override
		public void actionPerformed(ActionEvent e) {

			Set<Waypoint> waypoints = new HashSet<Waypoint>();

			if (scanner != null) {
				try {
					Integer time;
					Integer size;
					Integer minutes;

					time = scanner.nextInt();
					minutes = scanner.nextInt();
					size = scanner.nextInt();

					Long ref_from = 0L;
					Long ref_to = 0L;
					Integer step = 0;
					Integer maxstep = 1;
					Integer type = 0;
					Double lat, lon;
					Double lat2, lon2;
					Integer num_captured_gangsters;
					String name = "Cop";

					java.util.Map<String, Integer> cops = new java.util.HashMap<String, Integer>();

					for (int i = 0; i < size; ++i) {

						ref_from = scanner.nextLong();
						ref_to = scanner.nextLong();
						maxstep = scanner.nextInt();
						step = scanner.nextInt();
						type = scanner.nextInt();

						if (type == 1) {
							num_captured_gangsters = scanner.nextInt();
							name = scanner.next();

							if (cops.containsKey(name)) {
								cops.put(name, cops.get(name) + num_captured_gangsters);
							} else {
								cops.put(name, num_captured_gangsters);
							}
						}

						lat = lmap.get(ref_from).getLatitude();
						lon = lmap.get(ref_from).getLongitude();

						lat2 = lmap.get(ref_to).getLatitude();
						lon2 = lmap.get(ref_to).getLongitude();

						if (maxstep == 0) {
							maxstep = 1;
						}

						lat += step * ((lat2 - lat) / maxstep);
						lon += step * ((lon2 - lon) / maxstep);

						if (type == 1) {
							waypoints.add(new WaypointPolice(lat, lon, name));
						} else if (type == 2) {
							waypoints.add(new WaypointGangster(lat, lon));
						} else if (type == 3) {
							waypoints.add(new WaypointCaught(lat, lon));
						} else {
							waypoints.add(new org.jxmapviewer.viewer.DefaultWaypoint(lat, lon));
						}

					}

					if (time >= minutes * 60 * 1000 / 200) {
						scanner = null;
					}

					StringBuilder sb = new StringBuilder();

					int sec = time / 5;
					int min = sec / 60;
					sec = sec - min * 60;
					time = time - min * 60 * 5 - sec * 5;

					sb.append("|");
					sb.append(min);
					sb.append(":");
					sb.append(sec);
					sb.append(":");
					sb.append(2 * time);
					sb.append("|");
					// sb.append(" Justine - Car Window (log player for Robocar City Emulator, Robocar World Championshin in Debrecen)");
					sb.append(java.util.Arrays.toString(cops.entrySet().toArray()));

					setTitle(sb.toString());
					waypointPainter.setWaypoints(waypoints);

					jxMapViewer.repaint();
					repaint();

				} catch (InputMismatchException imE) {
					LOGGER.error("Hibás bemenet...");
				} catch (NoSuchElementException e1) {
					LOGGER.error("Tervezett leállás: input végét kapott el a kivételkezelő.");
					CopController.this.dispatchEvent(new WindowEvent(CopController.this, WindowEvent.WINDOW_CLOSING));
				}
			}
		}
	};

	public CopController(Double lat, Double lon, java.util.Map<Long, GPSLocation> lmap, String hostname, int port) {
		this.lmap = lmap;
		this.hostname = hostname;
		this.port = port;

		final TileFactory tileFactoryArray[] = { new DefaultTileFactory(new OSMTileFactoryInfo()),
				new DefaultTileFactory(new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.MAP)),
				new DefaultTileFactory(new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.SATELLITE)),
				new DefaultTileFactory(new VirtualEarthTileFactoryInfo(VirtualEarthTileFactoryInfo.HYBRID)) };

		GeoPosition debrecen = new GeoPosition(lat, lon);

		MouseInputListener mouseListener = new PanMouseInputListener(jxMapViewer);
		jxMapViewer.addMouseListener(mouseListener);
		jxMapViewer.addMouseMotionListener(mouseListener);
		jxMapViewer.addMouseListener(new CenterMapListener(jxMapViewer));
		jxMapViewer.addMouseWheelListener(new ZoomMouseWheelListenerCursor(jxMapViewer));
		jxMapViewer.addKeyListener(new PanKeyListener(jxMapViewer));
		jxMapViewer.setTileFactory(tileFactoryArray[0]);

		ClassLoader classLoader = this.getClass().getClassLoader();

		final java.awt.Image markerImg = new javax.swing.ImageIcon(classLoader.getResource("logo1.png")).getImage();
		final java.awt.Image markerImgPolice = new javax.swing.ImageIcon(classLoader.getResource("logo2.png")).getImage();
		final java.awt.Image markerImgGangster = new javax.swing.ImageIcon(classLoader.getResource("logo3.png")).getImage();
		final java.awt.Image markerImgCaught = new javax.swing.ImageIcon(classLoader.getResource("logo4.png")).getImage();

		waypointPainter.setRenderer(new WaypointRenderer<Waypoint>() {

			@Override
			public void paintWaypoint(Graphics2D g2d, JXMapViewer jxMapViewer, Waypoint waypoint) {
				Point2D point = jxMapViewer.getTileFactory().geoToPixel(waypoint.getPosition(), jxMapViewer.getZoom());

				if (waypoint instanceof WaypointPolice) {
					g2d.drawImage(markerImgPolice, (int) point.getX() - markerImgPolice.getWidth(jxMapViewer), (int) point.getY()
							- markerImgPolice.getHeight(jxMapViewer), null);

					g2d.setFont(new Font("Serif", java.awt.Font.BOLD, 14));
					java.awt.FontMetrics fm = g2d.getFontMetrics();
					Integer nameWidth = fm.stringWidth(((WaypointPolice) waypoint).getName());
					g2d.setColor(Color.GRAY);
					Rectangle rect = new Rectangle((int) point.getX(), (int) point.getY(), nameWidth + 4, 20);
					g2d.fill(rect);
					g2d.setColor(Color.CYAN);
					g2d.draw(rect);
					g2d.setColor(Color.WHITE);
					g2d.drawString(((WaypointPolice) waypoint).getName(), (int) point.getX() + 2, (int) point.getY() + 20 - 5);
				} else if (waypoint instanceof WaypointGangster) {
					g2d.drawImage(markerImgGangster, (int) point.getX() - markerImgGangster.getWidth(jxMapViewer), (int) point.getY()
							- markerImgGangster.getHeight(jxMapViewer), null);
				} else if (waypoint instanceof WaypointCaught) {
					g2d.drawImage(markerImgCaught, (int) point.getX() - markerImgCaught.getWidth(jxMapViewer), (int) point.getY()
							- markerImgCaught.getHeight(jxMapViewer), null);
				} else {
					g2d.drawImage(markerImg, (int) point.getX() - markerImg.getWidth(jxMapViewer),
							(int) point.getY() - markerImg.getHeight(jxMapViewer), null);
				}

			}
		});

		jxMapViewer.setOverlayPainter(waypointPainter);
		jxMapViewer.setZoom(9);
		jxMapViewer.setAddressLocation(debrecen);
		jxMapViewer.setCenterPosition(debrecen);

		jxMapViewer.addKeyListener(new java.awt.event.KeyAdapter() {
			int index = 0;

			public void keyPressed(java.awt.event.KeyEvent evt) {

				if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_SPACE) {
					jxMapViewer.setTileFactory(tileFactoryArray[++index % 4]);
					jxMapViewer.repaint();
					repaint();
				}
			}
		});

		setTitle("Gergő - Car Window (Cop controller for Robocar City Emulator, Robocar World Championshin in Debrecen)");
		getContentPane().add(jxMapViewer);

		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

		setSize(screenDim.width / 2, screenDim.height / 2);
		setDefaultCloseOperation(javax.swing.JFrame.EXIT_ON_CLOSE);

		worker.execute();
	}

	public static void main(String[] args) throws CopControllerException {
		LOGGER.debug("Starting up the application.");

		final Map<Long, GPSLocation> locationMap = new HashMap<>();

		if (args.length == 1) {
			readMap(locationMap, args[0]);

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Entry<Long, GPSLocation> loc = locationMap.entrySet().iterator().next();
					new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, "localhost", 10007).setVisible(Boolean.TRUE);
				}
			});
		} else if (args.length == 2) {
			readMap(locationMap, args[0]);
			String hostname = args[1];
					
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Entry<Long, GPSLocation> loc = locationMap.entrySet().iterator().next();
					new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, hostname, 10007).setVisible(Boolean.TRUE);
				}
			});

		} else if (args.length == 3) {
			readMap(locationMap, args[0]);
			String hostname = args[1];
			Integer port = Integer.parseInt(args[2]);
			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Entry<Long, GPSLocation> loc = locationMap.entrySet().iterator().next();
					new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, hostname, port).setVisible(Boolean.TRUE);
				}
			});
		}
	}

}
