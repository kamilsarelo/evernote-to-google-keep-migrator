package com.kamilsarelo.evernotetogooglekeepmigrator;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.WindowConstants;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;

public final class View {

	// logger //////////////////////////////////////////////////////////////////////////////////////

	private final static Logger LOGGER = Logger.getLogger(View.class.getName());
	static {
		Utils.decorateLogger(LOGGER);
	}

	// lazy loading singleton //////////////////////////////////////////////////////////////////////

	private static volatile View instance = null;

	public static View getInstance() {
		if (instance == null) {
			synchronized (View.class) {
				if (instance == null) {
					instance = new View();
				}
			}
		}
		return instance;
	}

	// fields //////////////////////////////////////////////////////////////////////////////////////

	private final Rectangle monitorBounds;

	private final JFrame frame;

	private final JButton buttonMigrateSelectedNote;
	private final JButton buttonSelectNextNoteAndMigrate;
	private final JButton buttonClearKeep;

	// constructors ////////////////////////////////////////////////////////////////////////////////

	private View() {
		// https://stackoverflow.com/questions/4627553/show-jframe-in-a-specific-screen-in-dual-monitor-configuration
		monitorBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();

		frame = new JFrame("Evernote to Google Keep Migrator");

		buttonMigrateSelectedNote = new JButton("migrate the selected note in Evernote");
		buttonSelectNextNoteAndMigrate = new JButton("select the next note in Evernote and migrate it");
		buttonClearKeep = new JButton("delete all notes in Google Keep");

		// https://stackoverflow.com/questions/7613577/java-how-do-i-prevent-windowclosing-from-actually-closing-the-window
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		// https://stackoverflow.com/questions/13467997/disable-drag-of-jframe
		frame.setResizable(false);
		frame.getRootPane().setWindowDecorationStyle(JRootPane.PLAIN_DIALOG);
		frame.setUndecorated(true);
		frame.setAlwaysOnTop(true);

		layoutContent();
		createListeners();
		setButtonsEnabled(false);

		frame.pack();
		frame.setLocation(0, 0);
		frame.setSize(monitorBounds.width, frame.getSize().height);
		frame.setVisible(true);
	}

	// private methods /////////////////////////////////////////////////////////////////////////////

	private void layoutContent() {
		final JPanel componentButtons = new JPanel(new GridLayout(0, 3));
		componentButtons.add(buttonMigrateSelectedNote);
		componentButtons.add(buttonSelectNextNoteAndMigrate);
		componentButtons.add(buttonClearKeep);

		final Box componentMenu = Box.createHorizontalBox();
		componentMenu.add(Box.createHorizontalGlue());
		componentMenu.add(componentButtons);
		componentMenu.add(Box.createHorizontalGlue());

		final Container componentContent = frame.getContentPane();
		componentContent.setLayout(new BorderLayout());
		componentContent.add(componentMenu, BorderLayout.SOUTH);
	}

	private void createListeners() {
		frame.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				// https://stackoverflow.com/questions/7613577/java-how-do-i-prevent-windowclosing-from-actually-closing-the-window
				final int result = JOptionPane.showOptionDialog(
						frame,
						"Are you sure you want to quit?",
						"Confirm quit",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null,
						null,
						null);
				if (result == JOptionPane.YES_OPTION) {
					frame.dispose();
				}
			}

			@Override
			public void windowClosed(final WindowEvent event) {
				Controller.getInstance().onWindowClosed();
			}

		});

		buttonMigrateSelectedNote.addActionListener(e -> {
			new Thread(() -> Controller.getInstance().migrateSelectedEvernoteNoteToKeepNote()).start();
		});

		buttonSelectNextNoteAndMigrate.addActionListener(e -> {
			new Thread(() -> Controller.getInstance().selectNextEvernoteNoteAndMigrateToKeepNote()).start();
		});

		buttonClearKeep.addActionListener(e -> {
			new Thread(() -> Controller.getInstance().deleteAllKeepNotes(true)).start();
		});
	}

	// public methods //////////////////////////////////////////////////////////////////////////////

	public void positionDrivers(
			final WebDriver driverEvernote,
			final WebDriver driverKeep) {

		EventQueue.invokeLater(() -> {
			final Rectangle frameBounds = frame.getBounds();
			final int driverY = frameBounds.y + frameBounds.height;

			driverEvernote.manage().window().setPosition(new org.openqa.selenium.Point(monitorBounds.x, driverY));
			driverEvernote.manage().window().setSize(new Dimension(monitorBounds.width / 2, monitorBounds.height - driverY));

			driverKeep.manage().window().setPosition(new org.openqa.selenium.Point(monitorBounds.x + monitorBounds.width / 2, driverY));
			driverKeep.manage().window().setSize(new Dimension(monitorBounds.width / 2, monitorBounds.height - driverY));
		});
	}

	public void setButtonsEnabled(final boolean isEnabled) {
		EventQueue.invokeLater(() -> {
			// https://stackoverflow.com/questions/858572/how-to-make-a-new-list-in-java/48673336#48673336
			List.of(buttonMigrateSelectedNote, buttonSelectNextNoteAndMigrate, buttonClearKeep)
					.forEach(button -> button.setEnabled(isEnabled));
		});
	}

}
