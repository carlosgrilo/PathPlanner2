package tsp;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;

import java.util.*;


//Tsp with Simulated Annealing fed with all node distances pre-calculated as with the Dynamic programming
public class TspGeneticWithPrecalc {

    private final int N, start;
    private final double[][] distance;
    private final List<Integer> tour = new ArrayList<>();
    private double minTourCost = Double.POSITIVE_INFINITY;
    private boolean ranSolver = false;

    public TspGeneticWithPrecalc(double[][] distance) {
        this(0, distance);
    }

    public TspGeneticWithPrecalc(int start, double[][] distance) {
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


    public List<Integer> muTation(List<Integer> solution){
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


    public void insertN(int[] incidencia, int n){
        boolean found=false;
        for (int i=1;i<=incidencia[0];i++)
            if (incidencia[i]==n)
                found=true;
        if (!found){
            incidencia[0]++;
            incidencia[incidencia[0]]=n;
        }
    }

    public List<Integer> crossOver2(List<Integer> parent1, List<Integer> parent2) {
        int size=parent1.size();
        int p1[]=parent1.stream().mapToInt(i->i).toArray();
        int p2[]=parent2.stream().mapToInt(i->i).toArray();

        int[][] conns =new int[size][5];
        int nsol[]=new int[size];

        for (int i=0;i<size;i++){
            if (i>0){
                insertN(conns[p1[i]-1],p1[i-1]);
                insertN(conns[p2[i]-1],p2[i-1]);
            }
            if (i<size-1){
                insertN(conns[p1[i]-1],p1[i+1]);
                insertN(conns[p2[i]-1],p2[i+1]);
            }
        }
        int curr=p1[0];
        int comp=1;
        while (comp<size){
            nsol[comp-1]=curr;
            int next=curr;
            int nnext=5;
            int nc=0;
            //procura o nó com menos ligações abertas
            for (int i=1;i<=conns[curr-1][0];i++){
                int no=conns[curr-1][i];
                nc=conns[no-1][0];
                if ((nc!=0)&&(nc<nnext)){
                        next = no;
                        nnext = conns[next - 1][0];

                }
            }
            //Se não existir nenhum, continua com o 1º dos outros com ligacoes
            if ((next==curr)&&(comp<size-1))
                for (int i=0;i<size;i++)
                    if(conns[i][0]!=0){
                        next=i+1;
                        break;
                    }
            conns[curr-1][0]=0;
            curr=next;
            comp++;

        }
        nsol[size-1]=curr;
        ArrayList newsol=new ArrayList<>();
        for (int i=0;i<size;i++)
            newsol.add(new Integer(nsol[i]));
        return newsol;
    }

    public List<Integer> crossOver(List<Integer> parent1, List<Integer> parent2) {

        HashMap<Integer,HashMap> incidence = new HashMap<>();
        HashMap<Integer,Integer> edges;
        for(int i=0;i< parent1.size();i++){
            edges = new HashMap<>();
            Integer node=parent1.get(i);
            if (i>0)
                edges.put(parent1.get(i-1),parent1.get(i-1));
            if (i<parent1.size()-1)
                edges.put(parent1.get(i+1),parent1.get(i+1));
            incidence.put(node,edges);
        }
        for(int i=0;i< parent2.size();i++){

            Integer node=parent2.get(i);
            if (incidence.get(node).isEmpty())
                edges = new HashMap<>();
            else
                edges=incidence.get(node);
            if (i>0)
                edges.put(parent2.get(i-1),parent2.get(i-1));
            if (i<parent2.size()-1)
                edges.put(parent2.get(i+1),parent2.get(i+1));
            incidence.put(node,edges);
        }
        List<Integer> newSol = new ArrayList();
        Integer node= parent1.get(0);
        while (newSol.size()<parent1.size()-1){
            newSol.add(node);
            Integer nextnode=null;
            int mincount=N;
            for (Object aux: incidence.get(node).values()) {
                if (incidence.get(aux).size()<mincount) {
                    mincount = incidence.get(aux).size();
                    nextnode = (Integer) aux;
                }
                incidence.get(aux).remove(node);
            }
            incidence.remove(node);
            if (nextnode!=null)
                node=nextnode;
            else if (newSol.size()<parent1.size()-1)
                node=(Integer)incidence.keySet().stream().toArray()[0];
        }
        newSol.addAll(incidence.keySet());
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

    public ArrayList<List<Integer>> initialPopulation(int size){
        ArrayList pop = new ArrayList<>();
        ArrayList<Integer> solution = new ArrayList<>(N - 2);
        for (int i = 1; i < N - 1; i++) {
            solution.add(new Integer(i));
        }
        for (int i=0;i<size;i++) {

            ArrayList newsolution = new ArrayList<>(solution);
            Collections.shuffle(newsolution);
            pop.add(newsolution);
        }
        return pop;
    }

    public int tourNament(int n, double fitness[]){
        Random r = new Random();
        int indices[]=new int[n];
        for (int i=0; i<n; i++)
            indices[i]=r.nextInt(fitness.length);

        int best=indices[0];
        for (int i=1;i<n;i++)
            if (fitness[indices[i]]<fitness[best])
                best=indices[i];
        return best;
    }
    // Solves the traveling salesman problem and caches solution.
    public void solve() {

        if (ranSolver) return;
        Random r = new Random();
        int popsize=80;
        double probmut=0.3, probcross=0.85;
        int maxgenerations=N*100;
        int toursize=3;
        long tstart = System.currentTimeMillis();
        System.out.println("BEG");
        ArrayList<List<Integer>> pop = initialPopulation(popsize);
        double fitness[] = new double[popsize];
        for (int p=0; p<popsize; p++){
            fitness[p]=evaluateSolution(pop.get(p));
        }

        int best=0;
        for (int gen=0;gen<maxgenerations; gen++) {
            ArrayList<List<Integer>> newpop = new ArrayList<>();
            for (int p = 0; p < popsize; p++) {
                int p1 = tourNament(toursize, fitness);
                int p2 = tourNament(toursize, fitness);
                if (Math.random() < probcross) {
                    newpop.add(crossOver2(pop.get(p1), pop.get(p2)));
                } else if (fitness[p1] < fitness[p2])
                    newpop.add(pop.get(p1));
                else
                    newpop.add(pop.get(p2));
            }
            for (int p = 0; p < popsize; p++) {
                if (Math.random() < probmut)
                    newpop.set(p, muTation(newpop.get(p)));
            }
            pop=newpop;
            best=0;
            for (int p=0; p<popsize; p++){
                fitness[p]=evaluateSolution(pop.get(p));
                if (fitness[p]<fitness[best])
                    best=p;
            }
            System.out.println("fitness:"+fitness[best]);
        }
        System.out.println("END");
        minTourCost=fitness[best];
        tour.add(start);
        tour.addAll(pop.get(best));
        tour.add(N-1);
        ranSolver=true;
        long tend = System.currentTimeMillis();
        System.out.println("Genetic: " +
                (tend - tstart) + "ms");
    }


}
