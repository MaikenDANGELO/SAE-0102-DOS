import java.io.File;
import java.io.FileOutputStream;
import java.util.Scanner;

public class DosSend {
    final int FECH = 44100; // fréquence d'échantillonnage
    final int FP = 1000;    // fréquence de la porteuses
    final int BAUDS = 100;  // débit en symboles par seconde
    final int FMT = 16 ;    // format des données
    final int MAX_AMP = (1<<(FMT-1))-1; // amplitude max en entier
    final int CHANNELS = 1; // nombre de voies audio (1 = mono)
    final int[] START_SEQ = {1,0,1,0,1,0,1,0}; // séquence de synchro au début
    static final Scanner input = new Scanner(System.in); // pour lire le fichier texte
 
    long taille;                // nombre d'octets de données à transmettre
    double duree;              // durée de l'audio
    double[] dataMod;           // données modulées
    static char[] dataChar;            // données en char
    FileOutputStream outStream; // flux de sortie pour le fichier .wav


    /**
     * Constructor
     * @param path  the path of the wav file to create
     */
    public DosSend(String path){
        File file = new File(path);
        try{
            outStream = new FileOutputStream(file);
        } catch (Exception e) {
            System.out.println("Erreur de création du fichier");
        }
    }

    /**
     * Write a raw 4-byte integer in little endian
     * @param octets    the integer to write
     * @param destStream  the stream to write in
     */
    public void writeLittleEndian(int octets, int taille, FileOutputStream destStream){
        char poidsFaible;
        while(taille > 0){
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
        taille = (long) (FECH * duree);
        long nbBytes = taille * CHANNELS * FMT / 8;
    
        try {
            outStream.write(new byte[]{'R', 'I', 'F', 'F'});
    
            // Taille totale du fichier en octets (32 bits, little-endian)
            writeLittleEndian((int) (36 + nbBytes), 4, outStream);
    
            outStream.write(new byte[]{'W', 'A', 'V', 'E', 'f', 'm', 't', ' '});
    
            // Taille du format (16 pour PCM) (32 bits, little-endian)
            writeLittleEndian(16, 4, outStream);
    
            // Format du son (1 pour PCM) (16 bits, little-endian)
            writeLittleEndian(1, 2, outStream);
    
            // Nombre de voies audio (1 pour mono) (16 bits, little-endian)
            writeLittleEndian(CHANNELS, 2, outStream);
    
            // Fréquence d'échantillonnage (32 bits, little-endian)
            writeLittleEndian(FECH, 4, outStream);
    
            // Débit binaire (nombre d'octets par seconde) (32 bits, little-endian)
            writeLittleEndian(FECH * CHANNELS * FMT / 8, 4, outStream);
    
            // Bloc d'alignement (nombre d'octets pour un échantillon) (16 bits, little-endian)
            writeLittleEndian(CHANNELS * FMT / 8, 2, outStream);
    
            // Nombre de bits par échantillon (16 pour PCM 16 bits) (16 bits, little-endian)
            writeLittleEndian(FMT, 2, outStream);
    
            outStream.write(new byte[]{'d', 'a', 't', 'a'});
    
            // Taille des données audio en octets (32 bits, little-endian)
            writeLittleEndian((int) nbBytes, 4, outStream);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    


    /**
     * Write the data in the wav file
     * after normalizing its amplitude to the maximum value of the format (8 bits signed)
     */
    public void writeNormalizeWavData() {
        try {
            // Normaliser les échantillons à la valeur maximale du format
            double maxSample = getMaxSample(dataMod);
            for (int i = 0; i < dataMod.length; i++) {
                dataMod[i] = (dataMod[i] / maxSample) * MAX_AMP;
            }
    
            // Convertir les échantillons normalisés en octets
            byte[] dataBytes = new byte[dataMod.length * FMT / 8];
            for (int i = 0; i < dataMod.length; i++) {
                writeLittleEndian((int) dataMod[i], FMT / 8, outStream);
            }
    
            // Écrire les données audio normalisées dans le fichier WAV
            outStream.write(dataBytes);
            outStream.close();
        } catch (Exception e) {
            System.out.println("Erreur d'écriture");
        }
    }
    
    // Fonction utilitaire pour obtenir la valeur maximale des échantillons
    private double getMaxSample(double[] samples) {
        double maxSample = 0.0;
        for (double sample : samples) {
            maxSample = Math.max(maxSample, Math.abs(sample));
        }
        return maxSample;
    }

 /**
     * Read the text data to encode and store them into dataChar
     * @return the number of characters read
     */
    public static int readTextData(){
        /**à compléter */
        dataChar = input.nextLine().toCharArray();
        return dataChar.length;
    }
     /**
     * convert a char array to a bit array
     * @param chars
     * @return byte array containing only 0 & 1
     */
    public static Byte[] charToBits(char[] chars){
        String[] binaire = new String[chars.length];

        // Convert each character to its binary representation
        for (int i = 0; i < chars.length; i++) {
            binaire[i] = Integer.toBinaryString(chars[i]);
            // Ensure each binary representation is 8 bits long
            binaire[i] = String.format("%8s", binaire[i]).replace(' ', '0');
        }
    
        // Concatenate binary strings
        String binaryString = String.join("", binaire);
    
        // Convert the binary string to a Byte array
        Byte[] result = new Byte[binaryString.length()];
        for (int i = 0; i < binaryString.length(); i++) {
            result[i] = (byte) (binaryString.charAt(i) == '1' ? 1 : 0);
        }
    
        return result;
    }

    /**
     * Modulate the data to send and apply the symbol throughput via BAUDS and FECH.
     * @param bits the data to modulate
     */
    public void modulateData(Byte[] bits) {
        dataMod = new double[bits.length * BAUDS];
        int index = 0;
    
        for (int i = 0; i < bits.length; i++) {
            double phaseIncrement = 2.0 * Math.PI * FP / FECH;
    
            for (int j = 0; j < BAUDS; j++) {
                double modulation = bits[i] == 1 ? Math.cos(phaseIncrement * j) : 1.0;
                dataMod[index++] = (byte) ((double) bits[i] * modulation);
            }
        }
    }
    

    /**
     * Display a signal in a window
     * @param sig  the signal to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    public static void displaySig(double[] sig, int start, int stop, String mode, String title){
      /*
          À compléter
      */
    }

    /**
     * Display signals in a window
     * @param listOfSigs  a list of the signals to display
     * @param start the first sample to display
     * @param stop the last sample to display
     * @param mode "line" or "point"
     * @param title the title of the window
     */
    //public static void displaySig(List<double[]> listOfSigs, int start, int stop, String mode, String title){
      /*
          À compléter
      */
    //}


    public static void main(String[] args) {
        // créé un objet DosSend
        DosSend dosSend = new DosSend("DosOok_message.wav");
        // lit le texte à envoyer depuis l'entrée standard
        // et calcule la durée de l'audio correspondant
        dosSend.duree = (double)(dosSend.readTextData()+dosSend.START_SEQ.length/8)*8.0/dosSend.BAUDS;

        // génère le signal modulé après avoir converti les données en bits
        dosSend.modulateData(dosSend.charToBits(dosSend.dataChar));
        // écrit l'entête du fichier wav
        dosSend.writeWavHeader();
        // écrit les données audio dans le fichier wav
        dosSend.writeNormalizeWavData();

        // affiche les caractéristiques du signal dans la console
        System.out.println("Message : "+String.valueOf(dosSend.dataChar));
        System.out.println("\tNombre de symboles : "+dosSend.dataChar.length);
        //System.out.println("\tNombre d'échantillons : "+dosSend.dataMod.length);
        System.out.println("\tDurée : "+dosSend.duree+" s");
        System.out.println();

        // exemple d'affichage du signal modulé dans une fenêtre graphique
        //displaySig(dosSend.dataMod, 1000, 3000, "line", "Signal modulé");
    }
}
