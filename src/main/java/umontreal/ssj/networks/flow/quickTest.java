package umontreal.ssj.networks.flow;

import umontreal.ssj.rng.LFSR113;
import umontreal.ssj.rng.RandomStream;

public class quickTest {

	
	public static void main(String[] args) {
		GraphFlow Do = ExamplesGraphs.buildDodecaNoOr(); //Attention, aucune capacit� set
		
		Do.setSource(0);
		Do.setTarget(19);
		RandomStream stream = new LFSR113();
		int b = 4;
		int demande = 5;
		double rho = 0.7;
		double[] epsilon = {1.0e-4, 1.0e-5, 1.0e-6, 1.0e-7, 1.0e-8, 1.0e-9, 1.0e-10, 1.0e-11,
				1.0e-12, 1.0e-13};
		
		PMCFlowNonOriented p = new PMCFlowNonOriented(Do);

		int m0 = p.father.getNumLinks();
		int[] tab = new int[m0];
		for (int i = 0; i<m0;i++) {
			tab[i] = b;
		}
		
		p.initCapaProbaB(tab, rho, epsilon[2]);
		p.trimCapacities(demande);
		
		p.filterOutside = true;
		p.frequency = 5;
		p.seuil = 0.8;
		
		p.run(50000, stream, demande);
		stream.resetStartSubstream();
		
		//p.filter = false;
		
		//p.run(50000, stream, demande);
		
		//p.runOld(500000, stream, demande);
		//ExamplesGraphs.toString(Latt6);
		
		//Comparaison avec Monte Carlo : inefficace � epsilon = 1e-4
		
		//MonteCarloFlowNonOriented mc = new MonteCarloFlowNonOriented(p.father);
		//stream.resetStartSubstream();
		//mc.run(500000, stream, demande);
		
		
	}
}
