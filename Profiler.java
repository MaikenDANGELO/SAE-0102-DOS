import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Function;

public class Profiler {

    private static long globalTime;
    private static int callCount;

    public static void init() {
        globalTime = 0;
        callCount = 0;
    }

    public static String getGlobalTime() {
        double elapsed = globalTime / 1e9;
        String unit = "s";
        if (elapsed < 1.0) {
            elapsed *= 1000.0;
            unit = "ms";
        }
        return String.format("%.4g%s elapsed", elapsed, unit);
    }

    @FunctionalInterface
interface LPFilterFunction {
    double[] apply(double[] inputSignal, double sampleFreq, double cutoffFreq);
}


    public static void analyse(LPFilterFunction lpFilter, double[] inputSignal, double sampleFreq, double cutoffFreq) {
        long start = timestamp();
        lpFilter.apply(inputSignal, sampleFreq, cutoffFreq);
        long stop = timestamp();
        callCount++;
        globalTime += (stop - start);
    }

    public static int getCallCount() {
        return callCount;
    }
    public static void ajouterAuFichier(String cheminFichier, String nouvelleVariable) {
        try (BufferedReader reader = new BufferedReader(new FileReader(cheminFichier));
             BufferedWriter writer = new BufferedWriter(new FileWriter(cheminFichier, true))) {

            // Vérifier si le fichier existe déjà
            if (!new File(cheminFichier).exists()) {
                // Si le fichier n'existe pas, écrire simplement la nouvelle variable
                writer.write(nouvelleVariable);
            } else {
                // Lire le contenu existant du fichier
                String ancienContenu = reader.readLine();

                // Vérifier s'il y a un contenu existant
                if (ancienContenu != null && !ancienContenu.isEmpty()) {
                    // Ajouter une virgule si le fichier n'est pas vide
                    writer.write("; ");
                }

                // Ajouter la nouvelle variable
                writer.write(nouvelleVariable);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String timestamp(long clock0) {
        String result = null;
        if (clock0 > 0) {
            double elapsed = (System.nanoTime() - clock0) / 1e9;
            String unit = "";
            elapsed *= 1000.0;
            result = String.format("%.4g%s", elapsed, unit);
        }
        return result;
    }

    public static long timestamp() {
        return System.nanoTime();
    }
}