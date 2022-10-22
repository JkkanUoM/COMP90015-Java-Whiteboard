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

	ArrayList<String> users = new ArrayList<>();
	ArrayList<Drawable> drawables = new ArrayList<>();
	Point start;
	Drawable current;
	Color currentColor = Color.black;
	CanvasPane pane = new CanvasPane();
	ObjectOutputStream out;
	
	public Paint() {
		super("White Board");

		JTextArea txt_users = new JTextArea();
		JTextArea txt_chat = new JTextArea();
		JScrollPane scroll1 = new JScrollPane(txt_users, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		JScrollPane scroll2 = new JScrollPane(txt_chat);

		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll1, scroll2);
		split.setPreferredSize(new Dimension(200, getContentPane().getHeight()));

		txt_users.setText("test");
		txt_chat.setText("dkjafkl\nadjfkldja\nkajldl\n");

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
		
		Rectangle2D rect = new Rectangle2D.Double(10, 10, 50, 50);
		Ellipse2D circ = new Ellipse2D.Double(50, 50, 50, 50);

		pane.addMouseListener(this);
		pane.addMouseMotionListener(this);
		pane.addKeyListener(this);
		pane.setFocusable(true);
		pane.requestFocusInWindow();
	}
	
	private void send(Message message) {
		try {

			out.writeObject(message);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	private void connect(String username) {
        try {
			Socket socket = new Socket("localhost", 4321);
			out = new ObjectOutputStream(socket.getOutputStream());
			Thread thread = new Thread(() -> {
				try {
					ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
					out.writeObject(username);

					while(true) {
						Message m = (Message) in.readObject();
						Drawable d = m.getDrawable();
						Info info = m.getInfo();
						if(d != null) {
							System.out.println("Client: recieved new drawable. it has a shape " + d.getShape());
							drawables.add(d);
							SwingUtilities.invokeLater(this::repaint);
						}
						if(info != null) {
							if(info.getAction() == Info.LEFT) {
								users.remove(info.getUsername());
								status.setText(info.getUsername() + " has left the chat");
							}
							if(info.getAction() == Info.IN) {
								users.add(info.getUsername());
							}
							if(info.getAction() == Info.JOINED) {
								users.add(info.getUsername());
								status.setText(info.getUsername() + " has joined the chat");
							}

							// update the txt_user with the content of the users Arraylist
						}
						// if type == chat add it to the text ara


					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			});
			
			thread.start();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}

	public static void main(String[] args) {
		Paint p = new Paint();
		p.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		p.setSize(1000, 1000);
		
		String s = JOptionPane.showInputDialog("Please enter your username");
		p.connect(s);
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
				System.out.println("Current updatd");
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
