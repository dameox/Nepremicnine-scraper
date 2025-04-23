import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Interface {
    //adds frame and two panels
    JFrame frame = new JFrame("Interface");
    JPanel panelUpper = new JPanel();
    JPanel panelLower = new JPanel();

    //used to repeat the scrapes in an interval
    private static int seconds = 0;
    private static ScheduledExecutorService scheduler;

    private static int scrapeCount = 0;
    Property[] propertiesListOG;

    NepremnicnineScraper scraper = new NepremnicnineScraper();



    public void initialize() {

        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 600);

        panelUpper.setLayout(new BoxLayout(panelUpper, BoxLayout.Y_AXIS));
        panelUpper.setBackground(Color.WHITE);

        //adds a scroll bar to the upper Panel
        JScrollPane scrollPane = new JScrollPane(panelUpper);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        //adds start btn and timer to lower panel
        panelLower.setLayout(new FlowLayout(FlowLayout.LEFT));
        JButton button1 = new JButton("Start scraping");
        JLabel label1 = new JLabel("00:00");
        panelLower.add(button1);
        panelLower.add(label1);

        frame.add(scrollPane, BorderLayout.CENTER);
        frame.add(panelLower, BorderLayout.SOUTH);

        //Timer class for the timer haha
        Timer timer = new Timer(1000, (ActionEvent e) -> {
            seconds++;
            int mins = seconds / 60;
            int secs = seconds % 60;
            label1.setText(mins + ":" + String.format("%02d", secs));
        });

        //When the button1 is pressed
        button1.addActionListener((ActionEvent e) -> {

                //checks if the scheduler is active
                if (scheduler == null || scheduler.isShutdown() && button1.getText().equals("Start scraping")) {

                    timer.start();
                    scheduler = Executors.newScheduledThreadPool(1);

                    //starts scheduler to do scraping
                    scheduler.scheduleAtFixedRate(() -> {
                        System.out.println("Scraping done at: " + java.time.LocalDateTime.now());
                        doScrape();
                    }, 0,30, TimeUnit.MINUTES);

                    button1.setEnabled(true);
                    button1.setForeground(Color.RED);
                    button1.setText("STOP");

                }   else {
                    //stops the scraping
                    timer.stop();
                    label1.setText("00:00");
                    button1.setText("Start scraping");
                    button1.setForeground(Color.BLACK);
                    scheduler.shutdownNow();

                }

        });

        frame.setVisible(true);
    }




    public void doScrape () {
        //runs the browser and gets the info
        Property[] properties = scraper.scrapeProperties();

        //adds the first found properties on the first run of the browser
        if (scrapeCount == 0) {

            propertiesListOG = new Property[properties.length];

            for (int i = 0; i < properties.length; i++) {
                propertiesListOG[i] = properties[i];
            }


        }

        //SwingUtilities to update the UI
            SwingUtilities.invokeLater(() -> {

                boolean newCheck = false;
                panelUpper.removeAll();


                for (Property p : properties) {
                    //Compares if there are new properties
                    if(!propertiesListOG[0].description.equals(properties[0].description)) {

                        for (Property property : properties) {

                            if (!p.description.equals(property.description)) {
                                newCheck = true;
                                break;
                            }
                        }

                    }

                    JPanel propertyCard = new JPanel();
                    propertyCard.setLayout(new BoxLayout(propertyCard, BoxLayout.Y_AXIS));

                    //if the property is new its background is green if not white
                    if (newCheck){
                        propertyCard.setBackground(new Color(80, 200, 120));
                    } else {
                    propertyCard.setBackground(new Color(245, 245, 245));
                    }

                    //adds border to property card
                    propertyCard.setBorder(BorderFactory.createCompoundBorder(
                            BorderFactory.createLineBorder(new Color(200, 200, 200)),
                            new EmptyBorder(10, 10, 10, 10)
                    ));

                    propertyCard.setAlignmentX(Component.LEFT_ALIGNMENT);

                    //adds the title, description, price and link from the property class to the property card
                    JLabel title = new JLabel(p.placeName);
                    title.setFont(new Font("Arial", Font.BOLD, 14));

                    JLabel desc = new JLabel("<html>" + p.description + "</html>");
                    desc.setFont(new Font("Arial", Font.PLAIN, 12));

                    JLabel price = new JLabel(p.price);
                    price.setFont(new Font("Arial", Font.BOLD, 13));
                    price.setForeground(new Color(50, 120, 50));

                    JLabel link = new JLabel("<html><a href='" + p.link + "'>" + p.link + "</a></html>");
                    link.setFont(new Font("Arial", Font.PLAIN, 11));



                    propertyCard.add(title);
                    propertyCard.add(Box.createVerticalStrut(5));
                    propertyCard.add(desc);
                    propertyCard.add(Box.createVerticalStrut(5));
                    propertyCard.add(price);
                    propertyCard.add(Box.createVerticalStrut(5));
                    propertyCard.add(link);

                    panelUpper.add(Box.createVerticalStrut(10));
                    panelUpper.add(propertyCard);

                    newCheck = false;
                }

                panelUpper.repaint();
                panelUpper.revalidate();

                //updates the property list
                for (int i = 0; i < properties.length; i++) {
                    propertiesListOG[i] = properties[i];
                }

            });

            scrapeCount++;
        }

    }

