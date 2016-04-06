package com.nute.copcontroller.ui;

import static com.nute.copcontroller.commons.StaticUtils.readMap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.MouseInputAdapter;
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

import com.nute.copcontroller.commons.StaticUtils;
import com.nute.copcontroller.entities.GPSLocation;
import com.nute.copcontroller.entities.Traffic;
import com.nute.copcontroller.entities.WaypointCaught;
import com.nute.copcontroller.entities.WaypointGangster;
import com.nute.copcontroller.entities.WaypointPolice;
import com.nute.copcontroller.models.CopControllerException;
import com.nute.copcontroller.models.TelnetWrapper;

public class CopController extends JFrame {
	private static final long serialVersionUID = 1L;

	private static final Logger LOGGER = LoggerFactory.getLogger(CopController.class);

	private WaypointPainter<Waypoint> waypointPainter = new WaypointPainter<Waypoint>();
	private JXMapViewer jxMapViewer = new JXMapViewer();
	private Map<Long, GPSLocation> lmap;
	private Scanner scanner;
	private String hostname = "localhost";
	private Integer port = 10007;
	private Long selectedCop = null;
	private TelnetWrapper telnetSwingWorker;

	private SwingWorker<Void, Traffic> worker = new SwingWorker<Void, Traffic>() {

		@Override
		protected synchronized Void doInBackground() throws Exception {
			try {
				LOGGER.debug("Connected to: {}:{}", hostname, port);

				scanner = new Scanner(telnetSwingWorker.getInputStreamWithMessage("<disp>"));

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
					Long cop_id = null;
					String name = "Cop";

					Map<String, Integer> scores = new HashMap<>();

					for (int i = 0; i < size; ++i) {

						ref_from = scanner.nextLong();
						ref_to = scanner.nextLong();
						maxstep = scanner.nextInt();
						step = scanner.nextInt();
						type = scanner.nextInt();

						if (type == 1) {
							num_captured_gangsters = scanner.nextInt();
							cop_id = scanner.nextLong();
							name = scanner.next();

							if (scores.containsKey(name)) {
								scores.put(name, scores.get(name) + num_captured_gangsters);
							} else {
								scores.put(name, num_captured_gangsters);
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
							WaypointPolice w = new WaypointPolice(lat, lon, name, cop_id);
							if (w.getId().equals(selectedCop))
								w.setSelected(true);
							waypoints.add(w);
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
					sb.append(Arrays.toString(scores.entrySet().toArray()));

					publish(new Traffic(waypoints, sb.toString()));
				}
			} catch (IOException e) {
				LOGGER.error(e.getMessage());
				CopController.this.dispatchEvent(new WindowEvent(CopController.this, WindowEvent.WINDOW_CLOSING));
			}

			return null;
		}

		@Override
		protected void process(List<Traffic> traffics) {

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

		private static final long serialVersionUID = 1L;

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
					Long cop_id = null;
					String name = "Cop";

					Map<String, Integer> cops = new HashMap<String, Integer>();

					for (int i = 0; i < size; ++i) {

						ref_from = scanner.nextLong();
						ref_to = scanner.nextLong();
						maxstep = scanner.nextInt();
						step = scanner.nextInt();
						type = scanner.nextInt();

						if (type == 1) {
							num_captured_gangsters = scanner.nextInt();
							cop_id = scanner.nextLong();
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
							WaypointPolice w = new WaypointPolice(lat, lon, name, cop_id);
							if (w.getId().equals(selectedCop))
								w.setSelected(true);
							waypoints.add(w);
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
					sb.append(Arrays.toString(cops.entrySet().toArray()));

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

	public CopController(Double lat, Double lon, Map<Long, GPSLocation> lmap, String hostname, int port) throws UnknownHostException,
			IOException {
		this.lmap = lmap;
		this.hostname = hostname;
		this.port = port;

		telnetSwingWorker = new TelnetWrapper(hostname, port);

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
		jxMapViewer.addMouseListener(new MouseInputAdapter() {

			@Override
			public void mouseClicked(MouseEvent e) {
				LOGGER.debug("Mousebutton: {}", e.getButton());

				if (e.getButton() == MouseEvent.BUTTON1) {

					Point mouseClick = new Point(e.getX(), e.getY());
					GPSLocation gpsLocation = new GPSLocation(jxMapViewer.convertPointToGeoPosition(mouseClick));
					selectedCop = StaticUtils.selectClosestCop(lmap, waypointPainter.getWaypoints(), gpsLocation);
					LOGGER.debug("Nearest cop: {}", selectedCop);
				}
				else if(e.getButton() == MouseEvent.BUTTON3) {
					Point mouseClick = new Point(e.getX(), e.getY());
					GPSLocation gpsLocation = new GPSLocation(jxMapViewer.convertPointToGeoPosition(mouseClick));
					Long nodeTo = StaticUtils.getClosestNode(lmap, gpsLocation);
					
					try {
						StaticUtils.sendCop(lmap, waypointPainter.getWaypoints(), nodeTo);
					} catch (URISyntaxException | IOException | InterruptedException e1) {
						LOGGER.error(e1.getMessage());
					}
				}
			}
		});

		ClassLoader classLoader = this.getClass().getClassLoader();

		final Image markerImg = new ImageIcon(classLoader.getResource("logo1.png")).getImage();
		final Image markerImgPolice = new ImageIcon(classLoader.getResource("logo2.png")).getImage();
		final Image markerImgGangster = new ImageIcon(classLoader.getResource("logo3.png")).getImage();
		final Image markerImgCaught = new ImageIcon(classLoader.getResource("logo4.png")).getImage();

		waypointPainter.setRenderer(new WaypointRenderer<Waypoint>() {

			@Override
			public void paintWaypoint(Graphics2D g2d, JXMapViewer jxMapViewer, Waypoint waypoint) {
				Point2D point = jxMapViewer.getTileFactory().geoToPixel(waypoint.getPosition(), jxMapViewer.getZoom());

				if (waypoint instanceof WaypointPolice) {
					g2d.drawImage(markerImgPolice, (int) point.getX() - markerImgPolice.getWidth(jxMapViewer), (int) point.getY()
							- markerImgPolice.getHeight(jxMapViewer), null);

					g2d.setFont(new Font("Serif", Font.BOLD, 14));
					FontMetrics fm = g2d.getFontMetrics();

					Integer nameWidth;
					if (((WaypointPolice) waypoint).isSelected())
						nameWidth = fm.stringWidth(((WaypointPolice) waypoint).getName() + " - "
								+ ((WaypointPolice) waypoint).getId().toString() + " - SELECTED");
					else
						nameWidth = fm.stringWidth(((WaypointPolice) waypoint).getName() + " - "
								+ ((WaypointPolice) waypoint).getId().toString());
					g2d.setColor(Color.GRAY);
					Rectangle rect = new Rectangle((int) point.getX(), (int) point.getY(), nameWidth + 4, 20);
					g2d.fill(rect);
					g2d.setColor(Color.CYAN);
					g2d.draw(rect);
					g2d.setColor(Color.WHITE);
					if (((WaypointPolice) waypoint).isSelected())
						g2d.drawString(((WaypointPolice) waypoint).getName() + " - " + ((WaypointPolice) waypoint).getId().toString()
								+ " - SELECTED", (int) point.getX() + 2, (int) point.getY() + 20 - 5);
					else
						g2d.drawString(((WaypointPolice) waypoint).getName() + " - " + ((WaypointPolice) waypoint).getId().toString(),
								(int) point.getX() + 2, (int) point.getY() + 20 - 5);
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

		jxMapViewer.addKeyListener(new KeyAdapter() {
			int index = 0;

			public void keyPressed(KeyEvent evt) {

				if (evt.getKeyCode() == KeyEvent.VK_SPACE) {
					jxMapViewer.setTileFactory(tileFactoryArray[++index % 4]);
					jxMapViewer.repaint();
					repaint();
				}
			}
		});

		JMenuBar menuBar = new JMenuBar();
		JMenu menu = new JMenu("Control commands");
		JMenuItem menuItem1 = new JMenuItem("Add 1 cop.", KeyEvent.VK_1);
		menuItem1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_1, ActionEvent.ALT_MASK));
		JMenuItem menuItem10 = new JMenuItem("Add 10 cop.", KeyEvent.VK_2);
		menuItem10.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_2, ActionEvent.ALT_MASK));

		menuItem1.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String cmdPath = StaticUtils.getResourcePath() + "init_1_cop.sh";

					LOGGER.debug("Relative path: {}", cmdPath);
					Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", cmdPath });
					p.waitFor();

					StringBuffer output = new StringBuffer();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					String line = "";
					while ((line = reader.readLine()) != null) {
						output.append(line + "\n");
					}
					LOGGER.debug(output.toString());
					LOGGER.debug("Added 1 cop.");
				} catch (IOException e1) {
					LOGGER.error(e1.getMessage());
				} catch (InterruptedException e2) {
					LOGGER.error(e2.getMessage());
				}
			}
		});

		menuItem10.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					String cmdPath = StaticUtils.getResourcePath() + "init_10_cop.sh";
					LOGGER.debug("Relative path: {}", cmdPath);
					Process p = Runtime.getRuntime().exec(new String[] { "/bin/sh", cmdPath });
					p.waitFor();

					StringBuffer output = new StringBuffer();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

					String line = "";
					while ((line = reader.readLine()) != null) {
						output.append(line + "\n");
					}
					LOGGER.debug("Added 10 cops.");
				} catch (IOException e1) {
					LOGGER.error(e1.getMessage());
				} catch (InterruptedException e2) {
					LOGGER.error(e2.getMessage());
				}
			}
		});

		menu.add(menuItem1);
		menu.add(menuItem10);
		menuBar.add(menu);
		this.setJMenuBar(menuBar);

		setTitle("Gergő - Car Window (Cop controller for Robocar City Emulator, Robocar World Championshin in Debrecen)");
		getContentPane().add(jxMapViewer);

		Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();

		setSize(screenDim.width / 2, screenDim.height / 2);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		worker.execute();
	}

	public static void main(String[] args) throws CopControllerException {
		LOGGER.debug("Starting up the application.");

		final Map<Long, GPSLocation> locationMap = new HashMap<>();
		final String defaulHostname = "localhost";
		final Integer defaultPort = 10007;

		if (args.length == 1) {
			readMap(locationMap, args[0]);

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Entry<Long, GPSLocation> loc = locationMap.entrySet().iterator().next();
					try {
						new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, defaulHostname,
								defaultPort).setVisible(Boolean.TRUE);
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
					}
				}
			});
		} else if (args.length == 2) {
			readMap(locationMap, args[0]);
			String hostname = args[1];

			SwingUtilities.invokeLater(new Runnable() {

				@Override
				public void run() {
					Entry<Long, GPSLocation> loc = locationMap.entrySet().iterator().next();
					try {
						new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, hostname, defaultPort)
								.setVisible(Boolean.TRUE);
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
					}
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
					try {
						new CopController(loc.getValue().getLatitude(), loc.getValue().getLongitude(), locationMap, hostname, port)
								.setVisible(Boolean.TRUE);
					} catch (IOException e) {
						LOGGER.error(e.getMessage());
					}
				}
			});
		}
	}

}
