public class LPFilter1 {
    public double[] lpFilter(double[] inputSignal, double sampleFreq, double cutoffFreq) {
        long startTime = System.nanoTime();
        double[] filteredAudio = new double[inputSignal.length];
        double sum = 0.0;

        for (int i = 0; i < inputSignal.length; i++) {
            if (i >= cutoffFreq) {
                // Soustrait la valeur qui n'est plus incluse dans la fenêtre
                sum -= inputSignal[i - (int) cutoffFreq];
            }

            // Ajoute la nouvelle valeur à la fenêtre
            sum += inputSignal[i];

            // Calcule la moyenne glissante
            filteredAudio[i] = sum / Math.min(cutoffFreq, i + 1);
        }

        // Remplace les données audio par les données traitées par le filtre
        System.arraycopy(filteredAudio, 0, inputSignal, 0, inputSignal.length);
        String cheminFichier = "lpfilter1_temps.txt";
        Profiler.ajouterAuFichier(cheminFichier, Profiler.timestamp(startTime));
        return filteredAudio;
    }
}
