import java.io.*;
import java.util.Arrays;

public class DosRead {

    static final int FP = 1000;

    static final int BAUDS = 100;

    static final int[] START_SEQ = { 1, 0, 1, 0, 1, 0, 1, 0 };

    FileInputStream fileInputStream;

    int sampleRate = 44100;

    int bitsPerSample;

    int dataSize;

    double[] audio;

    int[] outputBits;

    char[] decodedChars;

    /**
     * 
     * Constructor that opens the FIlEInputStream
     * 
     * and reads sampleRate, bitsPerSample and dataSize
     * 
     * from the header of the wav file
     * 
     * @param path the path of the wav file to read
     * 
     */

    public void readWavHeader(String path) {
        byte[] header = new byte[44]; // The header is 44 bytes long

        try {
            fileInputStream = new FileInputStream(path);
            fileInputStream.read(header);

            // RIFF chunk descriptor
            String chunkID = new String(header, 0, 4);
            int chunkSize = byteArrayToInt(header, 4, 32);
            String format = new String(header, 8, 4);

            // Format subchunk
            String subchunk1ID = new String(header, 12, 4);
            int subchunk1Size = byteArrayToInt(header, 16, 32);
            int audioFormat = byteArrayToInt(header, 20, 16);
            int numChannels = byteArrayToInt(header, 22, 16);
            sampleRate = byteArrayToInt(header, 24, 32);
            int byteRate = byteArrayToInt(header, 28, 32);
            int blockAlign = byteArrayToInt(header, 32, 16);
            bitsPerSample = byteArrayToInt(header, 34, 16);

            // Data subchunk
            String subchunk2ID = new String(header, 36, 4);
            dataSize = byteArrayToInt(header, 40, 32);

            // Print or use the obtained information as needed
            System.out.println("Sample Rate: " + sampleRate + " Hz");
            System.out.println("Bits per Sample: " + bitsPerSample + " bits");
            System.out.println("Data Size: " + dataSize + " bytes");

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 
     * Helper method to convert a little-endian byte array to an integer
     * 
     * @param bytes  the byte array to convert
     * 
     * @param offset the offset in the byte array
     * 
     * @param fmt    the format of the integer (16 or 32 bits)
     * 
     * @return the integer value
     * 
     */

    private static int byteArrayToInt(byte[] bytes, int offset, int fmt) {

        if (fmt == 16)

            return ((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF);

        else if (fmt == 32)

            return ((bytes[offset + 3] & 0xFF) << 24) |

                    ((bytes[offset + 2] & 0xFF) << 16) |

                    ((bytes[offset + 1] & 0xFF) << 8) |

                    (bytes[offset] & 0xFF);

        else
            return (bytes[offset] & 0xFF);

    }

    /**
     * 
     * Read the audio data from the wav file
     * 
     * and convert it to an array of doubles
     * 
     * that becomes the audio attribute
     * 
     */

    public void readAudioDouble() {
        byte[] audioData = new byte[dataSize];

        try {
            fileInputStream.read(audioData);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Convert the audio data to an array of doubles
        audio = new double[dataSize / (bitsPerSample / 8)];
        int byteIndex = 0;

        // Depending on the number of bits per sample, read and convert the data
        if (bitsPerSample == 16) {
            for (int i = 0; i < audio.length; i++) {
                audio[i] = byteArrayToShort(audioData, byteIndex) / 32768.0; // Normalize to range [-1, 1]
                byteIndex += 2;
            }
        } else if (bitsPerSample == 8) {
            for (int i = 0; i < audio.length; i++) {
                audio[i] = audioData[byteIndex] / 128.0; // Normalize to range [-1, 1]
                byteIndex++;
            }
        } else {
            // Handle other bit depths if necessary
        }
        // Print out some values for debugging
        System.out.println("First few bytes of audio data: " + Arrays.toString(Arrays.copyOf(audioData, 16)));
        System.out.println("First few values of audio array: " + Arrays.toString(Arrays.copyOf(audio, 10)));

    }

    // Helper method to convert a little-endian byte array to a short
    private static short byteArrayToShort(byte[] bytes, int offset) {
        return (short) (((bytes[offset + 1] & 0xFF) << 8) | (bytes[offset] & 0xFF));
    }

    /**
     * 
     * Reverse the negative values of the audio array
     * 
     */

    public void audioRectifier() {
        for (int i = 0; i < audio.length; i++) {
            audio[i] = Math.abs(audio[i]);
        }
    }

    /**
     * 
     * Apply a low pass filter to the audio array
     * 
     * Fc = (1/2n)*FECH
     * 
     * @param n the number of samples to average
     * 
     */

    public void audioLPFilter(int n) {
        double[] filteredAudio = new double[audio.length];

        // Appliquer le filtre passe-bas
        for (int i = 0; i < audio.length; i++) {
            double sum = 0.0;

            // Calculer la moyenne glissante sur n échantillons
            for (int j = Math.max(0, i - n + 1); j <= i; j++) {
                sum += audio[j];
            }

            filteredAudio[i] = sum / Math.min(n, i + 1);
        }

        // Remplacer le tableau audio avec les données filtrées
        System.arraycopy(filteredAudio, 0, audio, 0, audio.length);
        for(double a : filteredAudio){
            //System.out.print(a+" ");
        }
        System.out.println("");
    }

    /**
     * 
     * Resample the audio array and apply a threshold
     * 
     * @param period    the number of audio samples by symbol
     * 
     * @param threshold the threshold that separates 0 and 1
     * 
     */

     public void audioResampleAndThreshold(int period, int threshold) {
        int newSize = audio.length / period;
        outputBits = new int[newSize];
    
        double maxAmplitude = 0.0;
        for (int i = 0; i < audio.length; i++) {
            maxAmplitude = Math.max(maxAmplitude, Math.abs(audio[i]));
        }
    
        System.out.println("Max Amplitude: " + maxAmplitude);
        System.out.println("Threshold: " + threshold);
    
        for (int i = 0; i < newSize; i++) {
            // Calculate the average over the specified period
            double sum = 0.0;
            for (int j = i * period; j < (i + 1) * period && j < audio.length; j++) {
                sum += audio[j];
            }
            double average = (sum / Math.min(period, audio.length - i * period))*100000;
    
            System.out.println("Average: " + average);
    
            // Apply the dynamic threshold
            outputBits[i] = (Math.abs(average) >= threshold) ? 1 : 0;
        }
    
        for (int a : outputBits) {
            System.out.print(a + " ");
        }
        System.out.println("");
    }
    

    /**
     * 
     * Decode the outputBits array to a char array
     * 
     * The decoding is done by comparing the START_SEQ with the actual beginning of
     * outputBits.
     * 
     * The next first symbol is the first bit of the first char.
     * 
     */

    public void decodeBitsToChar() {
        // Trouver la séquence de synchronisation START_SEQ
        // Imprimez les valeurs binaires de outputBits
        System.out.print("outputBits: ");
        for (int bite : outputBits) {
            System.out.print(bite);
        }
        System.out.println(); // Ajoutez un saut de ligne pour une sortie propre

        // Trouvez la séquence de synchronisation START_SEQ
        int startIndex = -1;
        for (int i = 0; i <= outputBits.length - START_SEQ.length; i++) {
            if (isMatch(outputBits, i, START_SEQ)) {
                startIndex = i + START_SEQ.length;
                break;
            }
        }

        System.out.println("startIndex: " + startIndex);

        // Extraire les bits de données après la séquence de synchronisation
        if (startIndex != -1) {
            int dataSize = (outputBits.length - startIndex) / 8;
            decodedChars = new char[dataSize];

            for (int i = 0; i < dataSize; i++) {
                int byteValue = 0;
                for (int j = 0; j < 8; j++) {
                    byteValue = (byteValue << 1) | outputBits[startIndex + i * 8 + j];
                }
                decodedChars[i] = (char) byteValue;
            }
        } else {
            System.out.println("Séquence de synchronisation introuvable.");
            decodedChars = null;
        }
    }

    // Fonction utilitaire pour vérifier si une séquence correspond
    private static boolean isMatch(int[] bits, int startIndex, int[] sequence) {
        for (int i = 0; i < sequence.length; i++) {
            if (bits[startIndex + i] != sequence[i]) {
                return false;
            }
        }
        return true;
    }

    /**
     * 
     * Print the elements of an array
     * 
     * @param data the array to print
     * 
     */

    public static void printIntArray(char[] data) {
        for (char value : data) {
            System.out.print(value);
        }
        System.out.println(); // Ajouter un saut de ligne à la fin pour une sortie propre
    }

    /**
     * 
     * Display a signal in a window
     * 
     * @param sig   the signal to display
     * 
     * @param start the first sample to display
     * 
     * @param stop  the last sample to display
     * 
     * @param mode  "line" or "point"
     * 
     * @param title the title of the window
     * 
     */

    public static void displaySig(double[] sig, int start, int stop, String mode, String title) {
        // Initialiser StdDraw
        StdDraw.setXscale(0, sig.length);
        StdDraw.setYscale(-1, 1);
        StdDraw.setTitle(title);
        StdDraw.enableDoubleBuffering();
    
        // Dessiner le signal en fonction du mode spécifié
        if (mode.equals("line")) {
            for (int i = start; i < stop; i++) {
                StdDraw.line(i, sig[i], i + 1, sig[i + 1]);
            }
        } else if (mode.equals("point")) {
            for (int i = start; i < stop; i++) {
                StdDraw.point(i, sig[i]);
            }
        } else {
            System.out.println("Mode non pris en charge.");
        }
    
        // Attendre que l'utilisateur ferme la fenêtre
        StdDraw.show();
    }
    
    

    /**
     * 
     * Un exemple de main qui doit pourvoir être exécuté avec les méthodes
     * 
     * que vous aurez conçues.
     * 
     */

    public static void main(String[] args) {

        if (args.length != 1) {

            System.out.println("Usage: java DosRead <input_wav_file>");

            return;

        }

        String wavFilePath = args[0];

        // Open the WAV file and read its header

        DosRead dosRead = new DosRead();

        dosRead.readWavHeader(wavFilePath);

        // Print the audio data properties

        System.out.println("Fichier audio: " + wavFilePath);

        System.out.println("\tSample Rate: " + dosRead.sampleRate + " Hz");

        System.out.println("\tBits per Sample: " + dosRead.bitsPerSample + " bits");

        System.out.println("\tData Size: " + dosRead.dataSize + " bytes");

        // Read the audio data

        dosRead.readAudioDouble();

        // reverse the negative values

        dosRead.audioRectifier();

        // apply a low pass filter

        dosRead.audioLPFilter(44);

        // Resample audio data and apply a threshold to output only 0 & 1

        dosRead.audioResampleAndThreshold(dosRead.sampleRate / BAUDS, 12000);

        dosRead.decodeBitsToChar();

        if (dosRead.decodedChars != null) {

            System.out.print("Message décodé : ");

            printIntArray(dosRead.decodedChars);

        }

        displaySig(dosRead.audio, 0, dosRead.audio.length - 1, "line", "Signal audio");

        // Close the file input stream

        try {

            dosRead.fileInputStream.close();

        } catch (IOException e) {

            e.printStackTrace();

        }

    }

}