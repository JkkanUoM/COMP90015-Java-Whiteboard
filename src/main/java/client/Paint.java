package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class Paint extends JFrame implements MouseMotionListener, MouseListener, KeyListener {

	static final int ELLIPSE = 1;
	static final int RECTANGLE = 2;
	static final int LINE = 3;
	static final int TEXT = 4;
	static final int FREE = 5;
	static final int TRIANGLE = 6;
	
	int selected = RECTANGLE;
	JLabel status = new JLabel();
	DefaultListModel<String> userList = new DefaultListModel<>();
	ArrayList<Drawable> drawables = new ArrayList<>();
	Point start;
	Drawable current;
	Color currentColor = Color.black;
	CanvasPane pane = new CanvasPane();
	ObjectOutputStream out;
	Info selfInfo;
	
	public Paint() {
		super("White Board");

		JTextArea txt_chat = new JTextArea();
		JList<String> userJList = new JList<>(userList);
		JScrollPane scroll1 = new JScrollPane(userJList, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JScrollPane scroll2 = new JScrollPane(txt_chat);

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll1, scroll2);
		split.setPreferredSize(new Dimension(200, getContentPane().getHeight()));

		JPanel north = new JPanel();
		JColorChooser chooser = new JColorChooser();

		JButton btn_ellipse = new JButton("Circle");
		JButton btn_free = new JButton("Free Hand");
		JButton btn_rect = new JButton("Rectangle");
		JButton btn_line = new JButton("Line");
		JButton btn_text = new JButton("Text");
		JButton btn_color = new JButton("Color");
		JButton btn_triangle = new JButton("Triangle");

		// add small text field
		// add a chat button
		// chat button  send a chat message

		chooser.getSelectionModel().addChangeListener(System.out::println);

		btn_ellipse.addActionListener(e -> selected = ELLIPSE);
		btn_rect.addActionListener(e -> selected = RECTANGLE);
		btn_line.addActionListener(e -> selected = LINE);
		btn_text.addActionListener(e -> {
			selected = TEXT;
			status.setText("Click on the canvas and start typing");
		});
		btn_free.addActionListener(e -> selected = FREE);
		btn_triangle.addActionListener(e -> {
			selected = TRIANGLE;
			status.setText("Click on the canvas to add the first vertex of the triangle");
		});
		btn_color.addActionListener(e -> {
			Color c = JColorChooser.showDialog(null, "Select color", currentColor);
			if (c != null) {
				currentColor = c;
				if (current != null) {
					current.setColor(c);
				}
			}
		});
		userJList.addListSelectionListener(e -> {
			if (selfInfo.isManager) {
				String un = userJList.getSelectedValue();
				if (!un.equals(selfInfo.getUsername())) {
					Info info = new Info(userJList.getSelectedValue(), Info.IN);
					Object[] options = {"Yes", "Cancel"};
					int response = JOptionPane.showOptionDialog(this.getContentPane(),
							"Remove " + info.getUsername() + " from your board?",
							"Confirm Remove From Board",
							JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE,
							null,
							options,
							options[1]);
					if (response == 0) {
						info.setAction(Info.LEFT);
						send(new Message(info));
						userList.removeElement(un);
					}
				}
				userJList.clearSelection();
			}
		});

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(pane, BorderLayout.CENTER);
		getContentPane().add(north, BorderLayout.NORTH);
		getContentPane().add(status, BorderLayout.SOUTH);
		getContentPane().add(split, BorderLayout.EAST);
		north.add(btn_ellipse);
		north.add(btn_rect);
		north.add(btn_line);
		north.add(btn_text);
		north.add(btn_color);
		north.add(btn_free);
		north.add(btn_triangle);

		pane.addMouseListener(this);
		pane.addMouseMotionListener(this);
		pane.addKeyListener(this);
		pane.setFocusable(true);
		pane.requestFocusInWindow();
		pane.setEnabled(false);
	}
	
	private void send(Message message) {
		try {
			out.writeObject(message);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void connect(String username, int port) {
        try {
			Socket socket = new Socket("localhost", port);
			out = new ObjectOutputStream(socket.getOutputStream());
			Thread thread = new Thread(() -> {
				try {
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					out.writeObject(username);
					selfInfo = new Info(username, Info.JOINED);
					status.setText("Waiting for manager confirm...");

					while (true) {
						Message m = (Message) in.readObject();
						Drawable d = m.getDrawable();
						Info info = m.getInfo();
						if(d != null) {
							System.out.println("Client: recieved new drawable. it has a shape " + d.getShape());
							drawables.add(d);
							SwingUtilities.invokeLater(this::repaint);
						}
						if(info != null) {
							int act = info.getAction();
							String un = info.getUsername();

							if (un.equals(username)) {
								if (act == Info.LEFT) {
									out.close();
									in.close();
									socket.close();
									if (selfInfo.getAction() == Info.IN) {
										status.setText("Your connection is closed by manager.");
									} else {
										status.setText("Your name is already taken, choose another one.");
									}
									break;
								}
								if (act == Info.IN) {
									userList.addElement(username);
									selfInfo = info;
									pane.setEnabled(true);
									status.setText("You've joint the board!");
									continue;
								}
							}

							if (act == Info.LEFT) {
								userList.removeElement(un);
								if (info.isManager) {
									status.setText("Manager has left, the board is now disconnected, please close the window.");
									socket.close();
									break;
								}
								status.setText(un + " has left the board");
							}
							if (act == Info.IN) {
								userList.addElement(un);
								status.setText(info.getUsername() + " has joined the board");
							}
							if (act == Info.JOINED) {
								if (selfInfo.isManager) {
									int response = -1;
									if (userList.indexOf(un) == -1) {
										Object[] options = {"Confirm", "Decline"};
										response = JOptionPane.showOptionDialog(this.getContentPane(),
												un + " wants to join your board",
												"New Join Request",
												JOptionPane.YES_NO_OPTION,
												JOptionPane.QUESTION_MESSAGE,
												null,
												options,
												options[0]);
									}

									info.setAction(response == 0 ? Info.IN : Info.LEFT);
									send(new Message(info));
								}
							}

							// update the txt_user with the content of the users Arraylist
						}
						// if type == chat add it to the text ara


					}
				} catch (IOException | ClassNotFoundException e) {
					e.printStackTrace();
				}
			});
			
			thread.start();
			
		} catch (IOException e) {
			status.setText("Disconnected");
		}
        
	}

	public static void main(String[] args) {
		int port = 4321;
		if (args.length > 1) {
			try {
				port = Integer.parseInt(args[1]);
			} catch (NumberFormatException e) {
				System.out.println("Invalid port number");
				return;
			}
		}

		Paint p = new Paint();
		p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		p.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				try {
					if (p.selfInfo != null) {
						p.selfInfo.setAction(Info.LEFT);
						p.out.writeObject(new Message(p.selfInfo));
					}
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		});
		p.setSize(1000, 1000);
		
		String s = JOptionPane.showInputDialog("Please enter your username");
		p.connect(s, port);
		p.setVisible(true);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		if(start != null) {
			Point p = e.getPoint();
			double width = start.getX() - p.getX();
			double height = start.getY() - p.getY();
	
			// p is current cursor position
			// start is cursor original (where you clicked) position
			if (current != null) {
				switch (selected) {
	
				case LINE: {
					Line2D l = new Line2D.Double(start.getX(), start.getY(), p.getX(), p.getY());
					current.setShape(l);
					repaint();
					break;
				}
				
				case FREE:
					Path2D path = (Path2D) current.getShape();
					path.lineTo(p.getX(), p.getY());
					current.setShape(path);
					repaint();
					break;
					
				case RECTANGLE: {
	
					Rectangle2D old = (Rectangle2D) current.getShape();
	
					Rectangle2D r = null;
	
					if (width >= 0) {
						if (height >= 0) {
							r = new Rectangle2D.Double(p.getX(), p.getY(), width, height);
						} else {
							r = new Rectangle2D.Double(p.getX(), start.getY(), width, -height);
						}
					} else {
						if (height >= 0) {
	
							r = new Rectangle2D.Double(start.getX(), p.getY(), -width, height);
						} else {
							r = new Rectangle2D.Double(start.getX(), start.getY(), -width, -height);
						}
					}
					current.setShape(r);
					repaint();
					break;
				}
	
				case ELLIPSE:
					if (Math.abs(width) > Math.abs(height)) {
						if (height < 0) {
							height = -Math.abs(width);
						} else {
							height = Math.abs(width);
						}
					} else {
						if (width < 0) {
							width = -Math.abs(height);
						} else {
							width = Math.abs(height);
						}
					}
					Ellipse2D ellipse = null;
	
					if (width >= 0) {
						if (height >= 0) {
	
							ellipse = new Ellipse2D.Double(p.getX(), p.getY(), width, height);
						} else {
	
							ellipse = new Ellipse2D.Double(p.getX(), start.getY(), width, -height);
						}
					} else {
	
						if (height >= 0) {
	
							ellipse = new Ellipse2D.Double(start.getX(), p.getY(), -width, height);
						} else {
	
							ellipse = new Ellipse2D.Double(start.getX(), start.getY(), -width, -height);
						}
					}
					current.setShape(ellipse);
					repaint();
					break;
				}
			}
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {

	}

	@Override
	public void mouseClicked(MouseEvent e) {
		if (selected == TEXT) {
			pane.requestFocusInWindow();
			if (current == null) {
				System.out.println("Text mode");
				current = new Drawable("");
				current.setPoint(e.getPoint());
			}
		}
		else if(selected == TRIANGLE) {
			status.setText("Click on the point for the next vertex of the triangle");
			System.out.println(current);
			if(current == null) {
				Polygon poly = new Polygon();
				current = new Drawable(poly);
				poly.addPoint(e.getPoint().x, e.getPoint().y);
				current.setColor(currentColor);
				System.out.println("Current updated");
			}
			else {
				Polygon poly = (Polygon) current.getShape();
				System.out.println(poly.npoints);
				poly.addPoint(e.getPoint().x, e.getPoint().y);
				if(poly.npoints == 3) {
					send(new Message(current));
					status.setText("");
					current = null;
				}
			}
			repaint();
		}
	}

	@Override
	public void mousePressed(MouseEvent e) {
		if (current == null) {
			Point p = e.getPoint();

			switch (selected) {

			case RECTANGLE:
				start = p;
				Rectangle2D r = new Rectangle2D.Double(e.getX(), e.getY(), 0, 0);
				current = new Drawable(r);
				current.setColor(currentColor);
				break;
				
			case FREE:
				start = p;
				Path2D.Double path = new Path2D.Double();
				path.moveTo(p.getX(), p.getY());
				current = new Drawable(path);
				current.setColor(currentColor);
				break;
				
			case ELLIPSE:
				start = p;
			Ellipse2D ellipse = new Ellipse2D.Double(e.getX(), e.getY(), 0, 0);
				current = new Drawable(ellipse);
				current.setColor(currentColor);
				break;

			case LINE:
				start = p;
				Line2D line = new Line2D.Double(e.getX(), e.getY(), 0, 0);
				current = new Drawable(line);
				current.setColor(currentColor);
				break;
			// Line2d
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		if (current != null) {
			if(selected != TRIANGLE) {
				send(new Message(current));
				current = null;
			}
		}
	}

	@Override
	public void mouseEntered(MouseEvent e) {

	}

	@Override
	public void mouseExited(MouseEvent e) {

	}

	@Override
	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		if (current != null) {
			String s = current.getText();
			if (s != null) {
				current.setText(s + c);
				if(c == '\n') {
					send(new Message(current));
					current = null;
				}
				repaint();
			}
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {

	}

	@Override
	public void keyReleased(KeyEvent e) {

	}

	class CanvasPane extends JPanel {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			for (Drawable d : drawables) {
				if(d != null) {
					d.paint(g2d);
				}
			}

			if (current != null) {
				current.paint(g2d);
			}
		}
	}
}
