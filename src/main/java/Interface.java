import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Interface {
    JFrame frame = new JFrame("Interface");
    JPanel panelUpper = new JPanel();
    JPanel panelLower = new JPanel();

    private int seconds = 0;
    private ScheduledExecutorService scheduler;

    private static final int SCRAPE_INTERVAL_MINUTES = 30;

    private final Set<String> seenLinks = new HashSet<>();
    private boolean firstRun = true;

    private TrayIcon trayIcon;

    private final JLabel statusLabel = new JLabel("Idle");

    NepremnicnineScraper scraper = new NepremnicnineScraper();


    public void initialize() {

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        panelUpper.setLayout(new BoxLayout(panelUpper, BoxLayout.Y_AXIS));
        panelUpper.setBackground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(panelUpper);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        panelLower.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton button1 = new JButton("Start scraping");
        JLabel label1 = new JLabel("0:00");
        panelLower.add(button1);
        panelLower.add(label1);
        panelLower.add(statusLabel);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panelLower, BorderLayout.SOUTH);

        setupTrayIcon();

        Timer timer = new Timer(1000, (ActionEvent e) -> {
            seconds++;
            int mins = seconds / 60;
            int secs = seconds % 60;
            label1.setText(mins + ":" + String.format("%02d", secs));
        });

        button1.addActionListener((ActionEvent e) -> {

            if ((scheduler == null || scheduler.isShutdown())
                    && button1.getText().equals("Start scraping")) {

                seconds = 0;
                label1.setText("0:00");
                firstRun = true;
                seenLinks.clear();

                timer.start();
                scheduler = Executors.newScheduledThreadPool(1);

                scheduler.scheduleAtFixedRate(() -> {
                    System.out.println("Scraping done at: " + java.time.LocalDateTime.now());
                    doScrape();
                }, 0, SCRAPE_INTERVAL_MINUTES, TimeUnit.MINUTES);

                button1.setForeground(Color.RED);
                button1.setText("STOP");

            } else {
                timer.stop();
                seconds = 0;
                label1.setText("0:00");
                button1.setText("Start scraping");
                button1.setForeground(Color.BLACK);
                statusLabel.setText("Stopped");
                if (scheduler != null) {
                    scheduler.shutdownNow();
                }
            }
        });

        frame.setVisible(true);
    }


    public void doScrape() {
        final List<Property> all = new ArrayList<>();
        final List<Property> newOnes = new ArrayList<>();
        final boolean[] cleared = {false};

        SwingUtilities.invokeLater(() -> statusLabel.setText("Loading…"));

        scraper.scrapeProperties((pageProps, page, totalPages) ->
                SwingUtilities.invokeLater(() -> {
                    if (!cleared[0]) {
                        panelUpper.removeAll();
                        cleared[0] = true;
                    }

                    for (Property p : pageProps) {
                        boolean isNew = !firstRun && p.link != null
                                && !seenLinks.contains(p.link);
                        if (isNew) {
                            newOnes.add(p);
                        }
                        all.add(p);
                        panelUpper.add(Box.createVerticalStrut(10));
                        panelUpper.add(buildCard(p, isNew));
                    }

                    statusLabel.setText("Loading… (" + all.size() + " listings)");
                    panelUpper.revalidate();
                    panelUpper.repaint();
                }));

        SwingUtilities.invokeLater(() -> {
            for (Property p : all) {
                if (p.link != null) {
                    seenLinks.add(p.link);
                }
            }

            if (all.isEmpty()) {
                statusLabel.setText("No listings found (Cloudflare may be blocking)");
            } else {
                statusLabel.setText(all.size() + " listings • updated "
                        + java.time.LocalTime.now().withSecond(0).withNano(0));
            }

            if (!newOnes.isEmpty()) {
                notifyNewListings(newOnes);
            }

            firstRun = false;
        });
    }


    private JPanel buildCard(Property p, boolean isNew) {
        JPanel propertyCard = new JPanel();
        propertyCard.setLayout(new BoxLayout(propertyCard, BoxLayout.Y_AXIS));

        if (isNew) {
            propertyCard.setBackground(new Color(80, 200, 120));
        } else {
            propertyCard.setBackground(new Color(245, 245, 245));
        }

        propertyCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                new EmptyBorder(10, 10, 10, 10)
        ));

        propertyCard.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel title = new JLabel(p.placeName);
        title.setFont(new Font("Arial", Font.BOLD, 14));

        JLabel desc = new JLabel("<html>" + p.description + "</html>");
        desc.setFont(new Font("Arial", Font.PLAIN, 12));

        JLabel price = new JLabel(p.price);
        price.setFont(new Font("Arial", Font.BOLD, 13));
        price.setForeground(new Color(50, 120, 50));

        JLabel link = makeLinkLabel(p.link);

        propertyCard.add(title);
        propertyCard.add(Box.createVerticalStrut(5));
        propertyCard.add(desc);
        propertyCard.add(Box.createVerticalStrut(5));
        propertyCard.add(price);
        propertyCard.add(Box.createVerticalStrut(5));
        propertyCard.add(link);

        return propertyCard;
    }


    private JLabel makeLinkLabel(String url) {
        JLabel link = new JLabel("<html><a href=''>" + url + "</a></html>");
        link.setFont(new Font("Arial", Font.PLAIN, 11));
        link.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        link.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {
                    if (url != null && !url.isEmpty() && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI.create(url));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        return link;
    }


    private void setupTrayIcon() {
        if (!SystemTray.isSupported()) {
            return;
        }
        try {
            trayIcon = new TrayIcon(createTrayImage(), "Nepremicnine Scraper");
            trayIcon.setImageAutoSize(true);
            SystemTray.getSystemTray().add(trayIcon);
        } catch (AWTException ex) {
            ex.printStackTrace();
            trayIcon = null;
        }
    }


    private Image createTrayImage() {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new Color(80, 200, 120));
        g.fillRect(0, 0, 16, 16);
        g.dispose();
        return img;
    }


    private void notifyNewListings(List<Property> newOnes) {
        String title = newOnes.size() + " new listing" + (newOnes.size() == 1 ? "" : "s");

        StringBuilder body = new StringBuilder();
        int shown = Math.min(newOnes.size(), 3);
        for (int i = 0; i < shown; i++) {
            Property p = newOnes.get(i);
            body.append(p.placeName);
            if (p.price != null && !p.price.isEmpty()) {
                body.append(" — ").append(p.price);
            }
            body.append("\n");
        }
        if (newOnes.size() > shown) {
            body.append("…and ").append(newOnes.size() - shown).append(" more");
        }

        if (trayIcon != null) {
            trayIcon.displayMessage(title, body.toString().trim(), TrayIcon.MessageType.INFO);
        } else {
            JOptionPane.showMessageDialog(frame, body.toString().trim(), title,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
