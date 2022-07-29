package tsp;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;


//Tsp with Simulated Annealing fed with all node distances pre-calculated as with the Dynamic programming
public class TspSimulatedAnnealingWithPrecalc {

    private final int N, start;
    private final double[][] distance;
    private final List<Integer> tour = new ArrayList<>();
    private double minTourCost = Double.POSITIVE_INFINITY;
    private boolean ranSolver = false;

    public TspSimulatedAnnealingWithPrecalc(double[][] distance) {
        this(0, distance);
    }

    public TspSimulatedAnnealingWithPrecalc(int start, double[][] distance) {
        N = distance.length;

        if (N <= 2) throw new IllegalStateException("N <= 2 not yet supported.");
        if (N != distance[0].length) throw new IllegalStateException("Matrix must be square (n x n)");
        if (start < 0 || start >= N) throw new IllegalArgumentException("Invalid start node.");

        this.start = start;
        this.distance = distance;
    }

    // Returns the optimal tour for the traveling salesman problem.
    public List<Integer> getTour() {
        if (!ranSolver) solve();
        return tour;
    }

    // Returns the minimal tour cost.
    public double getTourCost() {
        if (!ranSolver) solve();
        return minTourCost;
    }


    public List<Integer> getNeighbour(List<Integer> solution){
        int i, j, range;
        Integer aux;
        range=solution.size();
        List<Integer> newSol = new ArrayList(solution);
        Random r = new Random();
        i=r.nextInt(range);
        j=r.nextInt(range);
        aux=newSol.get(i);
        newSol.set(i, newSol.get(j));
        newSol.set(j,aux);
        return newSol;
    }

    public double evaluateSolution(List<Integer> solution){
        double cost =0;
        int previous=start;
        for(int i=0;i<solution.size();i++){
                cost+=distance[previous][solution.get(i)];
                previous=solution.get(i);
        }
        cost+=distance[previous][N-1];
        return cost;
    }

    public List<Integer> initialSolution(){
        ArrayList<Integer> solution = new ArrayList<>(N-2);
        for (int i=1;i<N-1;i++){
            solution.add(new Integer(i));
        }
        Collections.shuffle(solution);
        return solution;
    }

    // Solves the traveling salesman problem and caches solution.
    public void solve() {

        if (ranSolver) return;
        long tstart = System.currentTimeMillis();
        int maxiteracoes=N*2500;
        int maxpassos=Math.min(25,N);
        double invTconst=7.0/maxiteracoes;
        Random r = new Random();
        r.setSeed(1);
        List<Integer> bestsolution= initialSolution();
        double bestval=evaluateSolution(bestsolution);

        for (int corrida=0;corrida<4;corrida++) {
            int lastiter=0;
            r.setSeed(corrida*1000);
            List<Integer> currentsolution = initialSolution();
            double currentval=evaluateSolution(currentsolution);
            double T0 = 100;

            for (int iteracao = 0; iteracao < maxiteracoes; iteracao++) {
                double T = T0 * Math.exp(-invTconst * iteracao);
                for (int passos = 0; passos < maxpassos; passos++) {
                    List<Integer> newsolution = getNeighbour(currentsolution);
                    double newval = evaluateSolution(newsolution);
                    if ((newval < currentval) || (Math.random() < Math.exp((currentval - newval) / T))) {
                        currentsolution = newsolution;
                        if ((newval<0.95*currentval)&&(iteracao>lastiter))
                            lastiter=iteracao;
                        currentval = newval;
                    }

                }

            }
            System.out.println("Last sig. improvement of run "+corrida+" at iteration "+lastiter);
            if (currentval<bestval){
                bestsolution=currentsolution;
                bestval=currentval;
            }

        }
        minTourCost=bestval;
        tour.add(start);
        tour.addAll(bestsolution);
        tour.add(N-1);
        ranSolver=true;
        long tend = System.currentTimeMillis();
        System.out.println("Simulated annealing: " +
                (tend - tstart) + "ms");
        System.out.println("Distance:"+bestval);
    }


}
