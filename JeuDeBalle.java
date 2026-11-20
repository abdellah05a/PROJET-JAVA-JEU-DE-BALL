import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Jeu de balle de type casse-briques, réalisable avec Java SE 11 uniquement.
 */
public final class JeuDeBalle extends JFrame {
    public JeuDeBalle() {
        super("Jeu de Balle - Casse-Briques");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(new PlateauJeu());
        pack();
        setLocationRelativeTo(null);
        setResizable(false);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new JeuDeBalle().setVisible(true));
    }
}

final class PlateauJeu extends JPanel implements ActionListener, KeyListener, MouseMotionListener {
    private static final int LARGEUR = 900;
    private static final int HAUTEUR = 620;
    private static final int PADDLE_LARGEUR = 130;
    private static final int PADDLE_HAUTEUR = 16;
    private static final int RAYON_BALLE = 10;
    private static final int NB_LIGNES = 5;
    private static final int NB_COLONNES = 10;
    private static final int ECART = 7;

    private final Timer minuteur = new Timer(16, this);
    private final List<Brique> briques = new ArrayList<>();
    private final Random hasard = new Random();
    private double balleX, balleY, vitesseX, vitesseY;
    private int paddleX;
    private int direction;
    private int score;
    private int vies;
    private int niveau;
    private boolean pause;
    private boolean fini;
    private boolean gagne;

    PlateauJeu() {
        setPreferredSize(new Dimension(LARGEUR, HAUTEUR));
        setBackground(new Color(12, 19, 38));
        setFocusable(true);
        addKeyListener(this);
        addMouseMotionListener(this);
        demarrerNouvellePartie();
        minuteur.start();
    }

    private void demarrerNouvellePartie() {
        score = 0;
        vies = 3;
        niveau = 1;
        fini = false;
        gagne = false;
        pause = false;
        creerNiveau();
        replacerBalle();
    }

    private void creerNiveau() {
        briques.clear();
        int largeurBrique = (LARGEUR - 100 - (NB_COLONNES - 1) * ECART) / NB_COLONNES;
        Color[] couleurs = {new Color(255, 99, 132), new Color(255, 159, 64),
                new Color(255, 205, 86), new Color(75, 192, 192), new Color(54, 162, 235)};
        for (int ligne = 0; ligne < NB_LIGNES; ligne++) {
            for (int colonne = 0; colonne < NB_COLONNES; colonne++) {
                int x = 50 + colonne * (largeurBrique + ECART);
                int y = 90 + ligne * 34;
                // À chaque niveau, quelques briques résistantes s'ajoutent.
                int solidite = (niveau > 1 && hasard.nextInt(10) < niveau) ? 2 : 1;
                briques.add(new Brique(x, y, largeurBrique, 25, couleurs[ligne], solidite));
            }
        }
    }

    private void replacerBalle() {
        paddleX = (LARGEUR - PADDLE_LARGEUR) / 2;
        balleX = LARGEUR / 2.0;
        balleY = HAUTEUR - 75;
        vitesseX = niveau % 2 == 0 ? 4.3 : -4.3;
        vitesseY = -5.1 - (niveau - 1) * 0.35;
    }

    @Override public void actionPerformed(ActionEvent evenement) {
        if (!pause && !fini) actualiser();
        repaint();
    }

    private void actualiser() {
        paddleX += direction * 9;
        paddleX = Math.max(12, Math.min(LARGEUR - PADDLE_LARGEUR - 12, paddleX));
        balleX += vitesseX;
        balleY += vitesseY;

        if (balleX - RAYON_BALLE <= 0 || balleX + RAYON_BALLE >= LARGEUR) {
            vitesseX = -vitesseX;
            balleX = Math.max(RAYON_BALLE, Math.min(LARGEUR - RAYON_BALLE, balleX));
        }
        if (balleY - RAYON_BALLE <= 48) {
            vitesseY = Math.abs(vitesseY);
            balleY = 48 + RAYON_BALLE;
        }

        Rectangle balle = new Rectangle((int) balleX - RAYON_BALLE, (int) balleY - RAYON_BALLE,
                RAYON_BALLE * 2, RAYON_BALLE * 2);
        Rectangle raquette = new Rectangle(paddleX, HAUTEUR - 44, PADDLE_LARGEUR, PADDLE_HAUTEUR);
        if (vitesseY > 0 && balle.intersects(raquette)) {
            double impact = (balleX - (paddleX + PADDLE_LARGEUR / 2.0)) / (PADDLE_LARGEUR / 2.0);
            vitesseX = impact * 6.8;
            vitesseY = -Math.abs(vitesseY);
            balleY = HAUTEUR - 44 - RAYON_BALLE;
        }

        for (int i = briques.size() - 1; i >= 0; i--) {
            Brique brique = briques.get(i);
            if (balle.intersects(brique.zone())) {
                brique.solidite--;
                vitesseY = -vitesseY;
                if (brique.solidite == 0) {
                    briques.remove(i);
                    score += 100;
                } else {
                    score += 25;
                }
                break;
            }
        }
        if (briques.isEmpty()) {
            if (niveau == 3) {
                fini = true;
                gagne = true;
            } else {
                niveau++;
                creerNiveau();
                replacerBalle();
                pause = true;
            }
        }
        if (balleY - RAYON_BALLE > HAUTEUR) {
            vies--;
            if (vies == 0) fini = true;
            else {
                replacerBalle();
                pause = true;
            }
        }
    }

    @Override protected void paintComponent(Graphics graphisme) {
        super.paintComponent(graphisme);
        Graphics2D g = (Graphics2D) graphisme.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        dessinerFond(g);
        dessinerInterface(g);
        for (Brique brique : briques) brique.dessiner(g);

        g.setColor(new Color(235, 245, 255));
        g.fillRoundRect(paddleX, HAUTEUR - 44, PADDLE_LARGEUR, PADDLE_HAUTEUR, 12, 12);
        g.setColor(new Color(54, 220, 255));
        g.fillOval((int) balleX - RAYON_BALLE, (int) balleY - RAYON_BALLE, RAYON_BALLE * 2, RAYON_BALLE * 2);
        if (pause || fini) dessinerMessage(g);
        g.dispose();
    }

    private void dessinerFond(Graphics2D g) {
        g.setPaint(new GradientPaint(0, 0, new Color(18, 31, 59), 0, HAUTEUR, new Color(5, 8, 19)));
        g.fillRect(0, 0, LARGEUR, HAUTEUR);
        g.setColor(new Color(255, 255, 255, 15));
        for (int x = 0; x < LARGEUR; x += 45) g.drawLine(x, 48, x, HAUTEUR);
    }

    private void dessinerInterface(Graphics2D g) {
        g.setColor(new Color(7, 12, 27, 220));
        g.fillRect(0, 0, LARGEUR, 48);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 18));
        g.drawString("SCORE : " + score, 22, 30);
        g.drawString("VIES : " + vies, 300, 30);
        g.drawString("NIVEAU : " + niveau + "/3", 490, 30);
        g.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g.drawString("← → ou souris : déplacer  |  ESPACE : pause  |  R : recommencer", 620, 29);
    }

    private void dessinerMessage(Graphics2D g) {
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(185, 235, 530, 135, 20, 20);
        g.setColor(Color.WHITE);
        g.setFont(new Font("SansSerif", Font.BOLD, 30));
        String titre = fini ? (gagne ? "BRAVO, VOUS AVEZ GAGNÉ !" : "PARTIE TERMINÉE") : "PAUSE";
        int largeur = g.getFontMetrics().stringWidth(titre);
        g.drawString(titre, (LARGEUR - largeur) / 2, 285);
        g.setFont(new Font("SansSerif", Font.PLAIN, 18));
        String aide = fini ? "Appuyez sur R pour rejouer" : "Appuyez sur ESPACE pour continuer";
        largeur = g.getFontMetrics().stringWidth(aide);
        g.drawString(aide, (LARGEUR - largeur) / 2, 330);
    }

    @Override public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_LEFT) direction = -1;
        if (e.getKeyCode() == KeyEvent.VK_RIGHT) direction = 1;
        if (e.getKeyCode() == KeyEvent.VK_SPACE && !fini) pause = !pause;
        if (e.getKeyCode() == KeyEvent.VK_R) demarrerNouvellePartie();
    }
    @Override public void keyReleased(KeyEvent e) {
        if ((e.getKeyCode() == KeyEvent.VK_LEFT && direction < 0) ||
            (e.getKeyCode() == KeyEvent.VK_RIGHT && direction > 0)) direction = 0;
    }
    @Override public void keyTyped(KeyEvent e) { }
    @Override public void mouseMoved(MouseEvent e) {
        paddleX = Math.max(12, Math.min(LARGEUR - PADDLE_LARGEUR - 12, e.getX() - PADDLE_LARGEUR / 2));
    }
    @Override public void mouseDragged(MouseEvent e) { mouseMoved(e); }
}

final class Brique {
    final int x, y, largeur, hauteur;
    final Color couleur;
    int solidite;
    Brique(int x, int y, int largeur, int hauteur, Color couleur, int solidite) {
        this.x = x; this.y = y; this.largeur = largeur; this.hauteur = hauteur;
        this.couleur = couleur; this.solidite = solidite;
    }
    Rectangle zone() { return new Rectangle(x, y, largeur, hauteur); }
    void dessiner(Graphics2D g) {
        Color affichage = solidite == 2 ? couleur.darker() : couleur;
        g.setColor(affichage);
        g.fillRoundRect(x, y, largeur, hauteur, 8, 8);
        g.setColor(new Color(255, 255, 255, 100));
        g.drawRoundRect(x, y, largeur, hauteur, 8, 8);
        if (solidite == 2) {
            g.setColor(Color.WHITE);
            g.setFont(new Font("SansSerif", Font.BOLD, 13));
            g.drawString("2", x + largeur / 2 - 4, y + 17);
        }
    }
}
