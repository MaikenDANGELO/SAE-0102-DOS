/*
 * Code source du groupe de Quentin BELHADJ et Maïken D'ANGELO du groupe S1C1.
 * Réalisé dans le cadre de la SAE 1-2.
 * Il peut arriver que le displaySig s'ouvre dans une petite fenêtre carrée n'affichant qu'une partie du signal. Pour régler le bug il suffit de relancer le programme.
 * Vous trouverez des preuves d'execution du jeu de test dans le fichier logs.txt
 */

import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000; // fréquence de la porteuses
    final int BAUDS = 100; // débit en symboles par seconde
    final int FMT = 16; // format des données
    final int MAX_AMP = (1 << (FMT - 1)) - 1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = { 1, 0, 1, 0, 1, 0, 1, 0 }; // séquence de synchro au début
    static final Scanner input = new Scanner(System.in); // pour lire le fichier texte

    long taille; // nombre d'octets de données à transmettre
    double duree; // durée de l'audio
    double[] dataMod; // données modulées
    static char[] dataChar; // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav

    /**
     * Constructor
     * 
     * @param path the path of the wav file to create
     */
    public DosSend(String path) {
        File file = new File(path);
        try {
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * 
     * @param octets     the integer to write
     * @param destStream the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream) {
        char poidsFaible;
        while (taille > 0) {
            poidsFaible = (char) (octets & 0xFF);
            try {
                destStream.write(poidsFaible);
            } catch (Exception e) {
                System.out.println("Erreur d'écriture");
            }
            octets = octets >> 8;
            taille--;
        }
    }

    /**
     * Create and write the header of a wav file
     *
     */
    public void writeWavHeader() {
        taille = (long) (FECH * duree); // Calcule la taille du fichier
        long nbBytes = taille * CHANNELS * FMT / 8; // Calcule le nombre de bytes dans le fichier

        try {
            // Ecrit RIFF dans l'entête du fichier
            outStream.write(new byte[] { 'R', 'I', 'F', 'F' });

            // Ecrit la taille totale du fichier en octets
            writeLittleEndian((int) (36 + nbBytes), 4, outStream);

            // Ecrit WAVEfmt dans l'entête du fichier
            outStream.write(new byte[] { 'W', 'A', 'V', 'E', 'f', 'm', 't', ' ' });

            // Ecrit la taille du format du fichier (16 pour PCM)
            writeLittleEndian(16, 4, outStream);

            // Ecrit le format du son (1 pour PCM)
            writeLittleEndian(1, 2, outStream);

            // Ecrit le nombre de voies audio (1 pour mono, 2 pour stéréo)
            writeLittleEndian(CHANNELS, 2, outStream);

            // Ecrit la fréquence d'échantillonnage
            writeLittleEndian(FECH, 4, outStream);

            // Ecrit le débit binaire (le nombre d'octets par seconde)
            writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);

            // Ecrit le nombre d'octets par échantillon
            writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);

            // Ecrit le nombre de bits par échantillon (16 pour PCM 16 bits)
            writeLittleEndian(FMT, 2, outStream);

            // Ecrit data dans l'entête de fichier pour marquer le début des données
            outStream.write(new byte[] { 'd', 'a', 't', 'a' });

            // Ecrit la taille des données en octets.
            writeLittleEndian((int) nbBytes, 4, outStream);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits
     * signed)
     */
    public void writeNormalizeWavData() {
        try {
            // Normalise les échantillons avec MAX_AMP
            double maxSample = getMaxSample(dataMod);
            for (int i = 0; i < dataMod.length; i++) {
                if (maxSample != 0)
                    dataMod[i] = (dataMod[i] / maxSample) * MAX_AMP;
            }

            // Convertit les échantillons normalisés en octets
            byte[] dataBytes = new byte[dataMod.length * FMT / 8];
            for (int i = 0; i < dataMod.length; i++) {
                writeLittleEndian((int) dataMod[i], FMT / 8, outStream);
            }

            // Ecrit les données normalisées dans le fichier
            outStream.write(dataBytes);
            outStream.close();
        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }

    /*
     * Fonction pour obtenir la valeur maximale parmi les échantillons
     */
    private double getMaxSample(double[] samples) {
        double maxSample = 0.0;
        for (double sample : samples) {
            maxSample = Math.max(maxSample, Math.abs(sample));
        }
        return maxSample;
    }

    /**
     * Read the text data to encode and store them into dataChar
     * 
     * @return the number of characters read
     */
    public static int readTextData() {
        /** à compléter */
        dataChar = input.nextLine().toCharArray();
        return dataChar.length;
    }

    /**
     * convert a char array to a bit array
     * 
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public static Byte[] charToBits(char[] chars) {
        String[] binaire = new String[chars.length];

        // Convertit chaque caractère en sa représentation binaire
        for (int i = 0; i < chars.length; i++) {
            binaire[i] = Integer.toBinaryString(chars[i]);
            // Permet de s'assurer que chaque représentation fait 8 bits
            binaire[i] = String.format("%8s", binaire[i]).replace(' ', '0');
        }

        // Concatenation des strings de représentation binaire
        String binaryString = String.join("", binaire);

        // Convertit le string de binaire en array de Byte
        Byte[] result = new Byte[binaryString.length()];
        for (int i = 0; i < binaryString.length(); i++) {
            result[i] = (byte) (binaryString.charAt(i) == '1' ? 1 : 0);
        }

        return result;
    }

    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * 
     * @param bits the data to modulate
     */
    public void modulateData(Byte[] bits) {
        int echantillonsParSymbole = FECH / BAUDS;
        dataMod = new double[(bits.length + START_SEQ.length) * echantillonsParSymbole];
        int index = 0;

        // Ajoute la séquence de démarrage aux début des données audio
        for (int i = 0; i < START_SEQ.length; i++) {
            double phaseIncrement = 2.0 * Math.PI * FP / FECH;

            for (int j = 0; j < echantillonsParSymbole; j++) {
                double modulation = START_SEQ[i] == 1 ? Math.cos(phaseIncrement * j) : 0.0;
                dataMod[index++] = modulation;
            }
        }

        // Modulation des données audio
        for (int i = 0; i < bits.length; i++) {
            double phaseIncrement = 2.0 * Math.PI * FP / FECH;

            for (int j = 0; j < echantillonsParSymbole; j++) {
                double modulation = bits[i] == 1 ? Math.cos(phaseIncrement * j) : 0.0;
                dataMod[index++] = modulation;
            }
        }
        long expectedEchantillons = (long) (FECH * duree);
        System.out.println("\nNombre d'échantillons attendu : " + expectedEchantillons);
    }

    /**
     * Display a signal in a window
     * 
     * @param sig   the signal to display
     * @param start the first sample to display
     * @param stop  the last sample to display
     * @param mode  "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        // Initialisation de StdDraw
        StdDraw.setCanvasSize(1280, 720);
        StdDraw.setXscale(start, stop);
        StdDraw.setYscale(-100000, 100000); // Echelle pour représenter correctement les valeurs de sig
        StdDraw.setTitle(title);
        StdDraw.setPenRadius(0.001);
        StdDraw.setPenColor(StdDraw.BLUE);
        StdDraw.enableDoubleBuffering();

        // Dessin du signal audio dans la fenêtre StdDraw
        if (mode.equals("line")) {
            for (int i = start; i < stop; i++) {
                StdDraw.line(i, sig[i], i + 1.0, sig[i + 1]);
            }
        } // Possibilité d'ajouter d'autres modes d'affichage

        StdDraw.show();
    }

    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (dosSend.readTextData() + dosSend.START_SEQ.length / 8.0) * 8.0 / dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : " + String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : " + dosSend.dataChar.length);
        System.out.println("\tNombre d'échantillons : " + dosSend.dataMod.length);
        System.out.println("\tDurée : " + dosSend.duree + " s");
        System.out.println();

        // exemple d'affichage du signal modulé dans une fenêtre graphique
        displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");
    }
}
