public class LPFilter2 {

    private double alpha;
    private double oldValue;

    /**
     * Initialise le filtre avec un paramètre alpha donnant le poids des échantillons précédents.
     * 
     * @param alpha Le paramètre alpha (0 < alpha < 1)
     */
    public LPFilter2(double alpha) {
        if (alpha <= 0.0 || alpha >= 1.0) {
            throw new IllegalArgumentException("Alpha doit être compris entre 0 et 1 exclus.");
        }
        this.alpha = alpha;
        this.oldValue = 0.0;
    }

    /**
     * Applique un filtre passe-bas sur le signal d'entrée.
     * 
     * @param inputSignal Le signal d'entrée
     * @return Le signal filtré
     */
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq){
        long startTime = System.nanoTime();
        double[] filteredSignal = new double[inputSignal.length];

        for (int i = 0; i < inputSignal.length; i++) {
            // Applique la moyenne exponentielle pondérée
            filteredSignal[i] = alpha * inputSignal[i] + (1 - alpha) * oldValue;
            oldValue = filteredSignal[i]; // Met à jour la valeur précédente
        }
        String cheminFichier = "lpfilter2_temps.txt";
        Profiler.ajouterAuFichier(cheminFichier, Profiler.timestamp(startTime));
        return filteredSignal;
    }
}
