package umontreal.ssj.networks.flow;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import umontreal.ssj.networks.GraphReliability;
import umontreal.ssj.networks.staticreliability.GraphWithForest;
import umontreal.ssj.networks.staticreliability.MarkovChainNetworkReliability;
import umontreal.ssj.networks.staticreliability.SamplerType;
import umontreal.ssj.probdist.ExponentialDist;
import umontreal.ssj.rng.RandomPermutation;
import umontreal.ssj.rng.RandomStream;
import umontreal.ssj.splitting.MarkovChainWithImportance;

import umontreal.ssj.splitting.MarkovChainWithImportance;


//creation de la MChain
//clonage A FAIRE

//Version Orient�e.
//Ensuite, pour Version non orient�e : il faut faire gaffe aux lambda et capacit�

public class MChainBis extends MarkovChainWithImportance {
	protected GraphFlow father;
	public MaxFlowEdmondsKarp Ek; //when chain is at level gamma, it keeps the info
	//about links added  before gamma
	public HashMap <Double,int[]> coordinates;
	protected RandomStream streamPermut; // for random permutations of links
	int demand;
	public ArrayList<Double> Yinf;  //retient les Yi,j inf�rieurs � gamma(t-1)
	public double[] valuesY;  //toutes les valeurs de Y


	public MChainBis(GraphFlow father, RandomStream streamPermut,
			int demand)
	{
		super();
		this.demand = demand;
		this.father=father;
		this.Ek = new MaxFlowEdmondsKarp(father);
		Ek.EdmondsKarp();
		this.streamPermut = streamPermut;
		this.coordinates  = new HashMap <Double,int[]>();
		this.valuesY = new double[1];this.valuesY[0]=0.0;
		Yinf = new ArrayList<Double>();
		
		for (int i=0;i<Ek.network.getNumLinks();i++) {
			father.initLinkLambda(i);
			Ek.network.setLambdaValues(father.getLambdaValues(i), i);
		}
		
	}
	
	
	public void initialState(RandomStream stream, double gamma) {
		int taille = 0; 
		for (int i=0;i<father.getNumLinks();i++) {
			father.initLinkLambda(i);
			
			//////////////PEUT FAIRE BUGGER EN NON COMMENTE
			
			//double[] tabY = new double[father.getLink(i).getB()];
			//father.getLink(i).setValuesY(tabY); // je m'en sers jamais ? mais initialise au moins(pas NULL)
			taille += father.getLink(i).getB();
		}
		//initialiser le drawing. On ne peut pas faire autrement
		Yinf = new ArrayList<Double>();
		valuesY = new double[taille];
		int compteur = 0;
		for (int i=0;i<father.getNumLinks();i++) {
			double [] lambI = father.getLambdaValues(i);
			for (int j=0;j< lambI.length;j++) {
				double lambda = lambI[j];
				valuesY[compteur] = ExponentialDist.inverseF(lambda, stream.nextDouble());
				int [] t = new int[2];
				t[0] = i; //System.out.println(i);
				t[1] = j; //System.out.println(j);
				coordinates.put(valuesY[compteur],t);
				//System.out.println(valuesY[compteur]);
				compteur++;
			}
		}
		Arrays.sort(valuesY);   //se demander si je garde �a
	}
	
	
	

	// juste la simulation conditionnelle des Y et leur modification.
	// PB, mettre � jour la structure qui concerne les gamma_t-1 et gamma_t
	//pour le test de re sampling, il faut faire increase cap et decrease cap
	//que faire des new Y?
	@Override
	public void nextStep(RandomStream stream, double gamma) {
		//System.out.println();
		//System.out.println("Next Step");
		//System.out.println();
		int numY = valuesY.length;
		int[] tab = new int[numY]; //tab contient des indices,
		//double [] newvaluesY = new double [numY]; // utile ?

		streamPermut.resetNextSubstream();
		RandomPermutation.init(tab, numY);
		RandomPermutation.shuffle(tab, streamPermut); // permute the links
		for (int l = 0; l < numY; l++) {
			int j = tab[l] - 1;
			double oldY = valuesY[j]; 
			System.out.println("Indice j " + j + " correspondant � y " + oldY );
			int [] indices = coordinates.get(oldY); //System.out.println(indices.length);
			int i = indices[0];
			int k = indices[1];
			System.out.println("Sanity check : indices " + i + " " + k);
			double newY;
			// test pour savoir si TC<gamma(t-1).  On verifie si Yi,k augmente la capa
			//on decrease cap dedans aussi(on la remet � la valeur initiale)
			//testIncreaseCap = false si newCap < oldCap

			//Si le nouveau Yik est plus petit que gamma(t-1), on ne fait pas de decrease cap
			// Si il est plus grand, on fait decrease cap
			//LinkFlow EdgeI  = father.getLink(i);
			LinkFlow EdgeI  = Ek.network.getLink(i);
			double lambda = EdgeI.getLambdaValue(k); // VERIFIER le k ou k+1
			
			double newYtemp = ExponentialDist.inverseF(lambda, stream.nextDouble()); //unconditional sampling
			
			
			boolean flowBiggerDemand; //We create a boolean to see if by changing Yi,k to 0, the
									//max flow is modified
			
			//first, if expected new capacity <= current capacity, false(max flow already to low)
			
			if (Ek.network.getLink(i).getCapacityValue(k+1)<=Ek.network.getLink(i).getCapacity()) {
				flowBiggerDemand = false;
			}
			else { //we need to test first
				//if augmenter la capa donne TC<gamma(t-1)
				//on doit aussi savoir si il faut remettre la capa initalement ou pas
				flowBiggerDemand = (testIncreaseCap(i,k));
			}
			
			System.out.println("Flow bigger demand ?" + flowBiggerDemand);
			
			if (flowBiggerDemand) {
				newY = gamma + newYtemp;
				//MAJ de values Y et coordinates ?	
				System.out.println("nouveau Y simul�" + newY);
				Yinf.remove(oldY);
				//System.out.println(Yinf.get(0));
				//System.out.println(Yinf.get(1));
				//System.out.println(Yinf.get(2));
				//System.out.println("oldY " +oldY);
				//System.out.println(gamma);
				//System.out.println("newY " +newY);
				
				valuesY[j] =newY;
				coordinates.put(newY, indices);
				coordinates.remove(oldY);
				//int [] test = coordinates.get(newY);
				//System.out.println("indice i " + i + " indice k " +k);
				//int a =test[0];
				
				
				
				
			}
			else { System.out.println("on est dans l'autre boucle");

				//LinkFlow EdgeI  = Ek.network.getLink(i);
				//double lambda = EdgeI.getLambdaValue(k);  // VERIFIER le k ou k+1
				//newY = ExponentialDist.inverseF(lambda, stream.nextDouble());
				newY = newYtemp;
				System.out.println("nouveau Y simul�" + newY);
				//MAJ de values Y et coordinates ? 
				if (newY <= gamma && oldY<= gamma) {
					System.out.println("On est dans cas newY inf�rieur et old Y inf�rieur");
					boolean removeY =Yinf.remove(oldY);
					Yinf.add(newY);

					valuesY[j] =newY;
					coordinates.put(newY, indices);
					coordinates.remove(oldY);
					
				}
				if (newY > gamma && oldY > gamma) {
					System.out.println("On est dans cas newY sup�rieur et old Y sup�rieur");
					valuesY[j] =newY;
					coordinates.put(newY, indices);
					coordinates.remove(oldY);
				}
				if ((newY <= gamma && oldY > gamma)) {
					System.out.println();
					System.out.println("On est dans cas newY inf�rieur et old Y sup�rieur");
					System.out.println();
					
					// PAS SUR QUE CE SOIT UTILE
					// CEST SUREMENT DEJA FAIT DANS TEST INCREASE CAP COMME NEWY<=GAMMA
					int prevCapacity = Ek.network.getLink(i).getCapacity();
					int newCapacity = Ek.network.getLink(i).getCapacityValue(k+1);
					
					if (prevCapacity<=newCapacity) {
					
					boolean reload = Ek.IncreaseLinkCapacity(i,newCapacity-prevCapacity);
					//System.out.println("prev capacity" + prevCapacity);
					//System.out.println("new capacity � set" + newCapacity);
					if (reload) {
						Ek.EdmondsKarp();
					}
					Ek.network.setCapacity(i, newCapacity);
					}
					//System.out.println("new capacity :" + Ek.network.getLink(i).getCapacity());
					Yinf.add(newY);
					valuesY[j] =newY;
					coordinates.remove(oldY);
					coordinates.put(newY, indices);
				}
				if ((newY > gamma && oldY <= gamma)) {
					System.out.println();
					System.out.println("ancien y appliqu�, nouveau y out");
					System.out.println();
					
					System.out.println("indice k ant�rieur " + k);
					System.out.println("correspond dans capValues �  " + Ek.network.getLink(i).getCapacityValue(k+1));
					System.out.println("En effet, la capacit� r�elle est" + Ek.network.getLink(i).getCapacity());
					boolean removeY =Yinf.remove(oldY);
					
					coordinates.put(newY, indices);
					coordinates.remove(oldY);
					//System.out.println();
					//System.out.println("On a enlev� oldY, il devrait etre parti");
					
					//System.out.println("oldY" + oldY);
					
					
					//System.out.println(removeY);
					//on parcourt les capacit�s de Yinf pour savoir � quelle capa diminuer
					//int kmax=-1; // ou 0 ?
					
					int prevCapacity = Ek.network.getLink(i).getCapacity();
					int possibleCapacity = Ek.network.getLink(i).getCapacityValue(k+1);
					
					//2 cas possibles. Soit il existe des Yi,k+1(ou autre) inf�rieurs � gamma. Le terme ne nous interesse pas.
					// SOit les Yi correspondent � des capacit�s plus faibles. Dans ce cas, il faut trouver � quel capa dans 
					//Yinf on dot diminuer
					
					if (prevCapacity==possibleCapacity) {
						int kmax = k;
						System.out.println("begin Yinf");
						for (int p=0;p<Yinf.size();p++) {
							//System.out.println("Yinf en p" + Yinf.get(p));
							int [] t = coordinates.get(Yinf.get(p));
							int i0 = t[0];
							int k0 = t[1];
							//System.out.println("p= " +p);
							//System.out.println("Y[p] " + Yinf.get(p));
							//System.out.println("i = " +i0);
							//System.out.println("k = " +k0);
							if (i0==i &&k0 <kmax) {
								System.out.println("On a trouv� un saut de capacit� inf�rieur, pour l'arete");
								System.out.println("indice i0 " + i0 + " indice k0 " +k0);
								System.out.println("indice i " + i);
								kmax = k0;
							}
						}
						if (kmax==k) {//on n'a pas trouv� de saut inf�rieur. Il faut revenir � la capacit�
							//initiale.
							kmax = -1;	
						}
						//System.out.println("end");
						//System.out.println("Kmax");
						//System.out.println(Ek.network.getLink(i).getCapacityValue(kmax));
						//System.out.println(Ek.network.getLink(i).getCapacityValue(kmax+1));
						//System.out.println("kmax" + kmax);
						int newC = Ek.network.getLink(i).getCapacityValue(kmax+1);
						System.out.println("newC " + newC);
						int oldC = Ek.network.getLink(i).getCapacity();
						System.out.println("oldC " + oldC);
						Ek.DecreaseLinkCapacity(i, oldC-newC);
						
						Ek.network.getLink(i).setCapacity(newC);
						//coordinates.put(newY, indices);
						//coordinates.remove(oldY);
						valuesY[j] =newY;
					}
					
			}
			}
			

			//
			//   // VERIFIER QUE indices ne meurt pas quand 
			//on eneleves oldY

			//dans la structure des Y inf�rieurs � gamma[t-1]

			//enlever le oldY. quelle influence sur les capacit�s ? d�pend


		}
			
		Arrays.sort(valuesY);//Trier valuesY? utile pour chercher
		//System.out.println();
		//System.out.println("Next step fini");
		//System.out.println("Valeurs de Y");
		//printTab(valuesY);
		//System.out.println();
		//System.out.println("To string du graphe");
		//System.out.println(Ek.network.toString());
		
	}



	public boolean testIncreaseCap(int i, int k) {
		//System.out.println();
		//System.out.println("test Increase Cap ");
		int prevCapacity = Ek.network.getLink(i).getCapacity();
		int newCapacity = Ek.network.getLink(i).getCapacityValue(k+1);
		System.out.println("prev capa" + prevCapacity);
		System.out.println("nouv capa" + newCapacity);
		boolean reload = Ek.IncreaseLinkCapacity(i,newCapacity-prevCapacity);
		if (reload) {
			Ek.EdmondsKarp();
		}
		//System.out.println("new capa" + newCapacity);
		int flow = Ek.maxFlowValue;
		System.out.println("max flow" + flow);
		System.out.println(Ek.network.toString());
		boolean test = (flow >= demand); //TC < gamma(t-1)
		Ek.DecreaseLinkCapacity(i, newCapacity-prevCapacity);
		return test;

	}



	//doit chercher les Y qui sont plus grands que gamma_t-1 et plus petits que gamma_t(=gamma),
	//pour les int�gr�er a la structure de maxFlot
	//on les rajoute dans Yinf et on met � jour Ek
	//values Y est suppos� sorted

	public void updateChainGamma (double gamma) {
		int taille = Yinf.size(); //nombre d'�lements y inf�rieurs � gamma(t-1). 0 initial
		//System.out.println(taille);
		int p = taille;
		//System.out.println(valuesY.length);
		//System.out.println("update avec Gamma :" + gamma);
		
		while (p<valuesY.length && valuesY[p]<=gamma) { 
			//System.out.println("true");
			double y = valuesY[p]; 
			//System.out.println("Valeur � rajouter" +y);
			//System.out.println();
			//System.out.println("Une valeur, check if sorted " +valuesY[p]);
			Yinf.add(valuesY[p]);
			//System.out.println("Valeur ajout�e" + Yinf.get(p));
			//System.out.println();
			int [] indices = coordinates.get(y);
			int i = indices[0];
			int k = indices[1];
			//System.out.println("Arete : " +i +" Indice : " + k);
			int prevCapacity = Ek.network.getLink(i).getCapacity();
			int newCapacity = Ek.network.getLink(i).getCapacityValue(k+1);
			Ek.network.setCapacity(i, newCapacity);
			//System.out.println("Capacit� maj dans network"+ Ek.network.getLink(i).getCapacity());
			boolean reload = Ek.IncreaseLinkCapacity(i,newCapacity-prevCapacity);
			//System.out.println("Update le flow ?" + reload);
			if (reload) {
				Ek.EdmondsKarp();
			}
			p++;
			//System.out.println("Max Flot " +Ek.maxFlowValue);
			//System.out.println();
			//System.out.println("Capacit� maj dans reisidual"+ Ek.residual.getLink(i).getCapacity());

		}
		//System.out.println("Max Flot � la fin de l'update " +Ek.maxFlowValue);
	}


	//Si on a fait le calcul du maxFlot � l'instant gam = gamma_t 
	// on le compare � la demande 
	@Override
	public boolean isImportanceGamma(double gam) {
		//System.out.println("max Flot vaut" + Ek.maxFlowValue);
		return Ek.maxFlowValue < demand;
	}

	



	@Override
	//calculer le TC
	//il faut que valuesY soit sorted. Verifier que c'est le cas
	
	public double getImportance() {
		System.out.println("=========DEBUT GET IMPORTANCE================");
		   //GraphFlow copy = father.clone();
		   int p =0;
		   int maxFlow = 0;
		   MaxFlowEdmondsKarp EkCopy = new MaxFlowEdmondsKarp(father);
		   while (maxFlow < demand && p<valuesY.length ) {
			   double y = valuesY[p];
			   System.out.println("VALEUR de y " + y);
			   int [] indices = coordinates.get(y);
			   int i = indices[0];
			   int k = indices[1];
			  // System.out.println("indices " + i + " " + k);
			   LinkFlow EdgeI = EkCopy.network.getLink(i);
			   int prevCapacity = EkCopy.network.getLink(i).getCapacity();
			  // System.out.println("ancien flow " + maxFlow);
			  // System.out.println("ancienne cap " + prevCapacity);
			   boolean reload =EkCopy.IncreaseLinkCapacity(i,EdgeI.getCapacityValue(k+1) - prevCapacity  );
			   EkCopy.network.getLink(i).setCapacity(EdgeI.getCapacityValue(k+1));
			  // System.out.println("nouvelle cap dans network " + EkCopy.network.getLink(i).getCapacity());
			  // System.out.println("nouvelle cap dans residual " + EkCopy.residual.getLink(i).getCapacity());
			   if (reload) {
				   maxFlow = EkCopy.EdmondsKarp();
			   }
			   //System.out.println("nouv flow " + maxFlow);
			   p++;	   
		   }
		   return valuesY[p-1]; // a verifier
	}

	@Override
	public double getPerformance() {
		// TODO Auto-generated method stub
		return 0;
	}
	
	

	
	// Verison de clonage direct, sans passer par valuesY
	@Override
	   public MChainBis clone() {
		   //MarkovChainRandomDiscreteCapacities image = (MarkovChainRandomDiscreteCapacities) super.clone(); 
		MChainBis image = new MChainBis(father,streamPermut,
			   		demand);
		   
		   //cloner les graphes du EK (on perd les capacit�s sinon et la structure de maxFlot)
		   image.Ek = this.Ek.clone();
		   
		   // Copier les Les lambda k . Il audra faire une MAJ de EK pour qu'il marche sur des GraphCapa
		   for (int i=0;i<image.father.getNumLinks();i++) {
			   double [] lamb = father.getLambdaValues(i);
			   image.Ek.network.setLambdaValues(lamb, i);
			   //image.Ek.residual.setLambdaValues(lamb, i);   
		   }
  
///CLONAGE DE VALUES Y(tous les Y)
		   
		  double [] copy = new double[this.valuesY.length];
		  //System.arraycopy(this.valuesY, 0, copy, 0, valuesY.length);
		  for (int i=0;i<valuesY.length;i++) {
			  copy[i] = valuesY[i];
		  }
		  image.valuesY = copy;
		  
// Clonage de la hash map
		  
		  //System.out.println("D�but clonage hash map");
		  
	        for (Map.Entry mapentry : this.coordinates.entrySet()) {
		           double key = (double) mapentry.getKey(); //est ce que ca marche ?
		           int[] t = this.coordinates.get(key);
		           int i = t[0];
		           int k = t[1];
		           int[] tab = new int[2]; tab[0] =i; tab[1]=k;
		           image.coordinates.put(key, tab);
	        		
	        }
	        
	        //System.out.println("Fin clonage hash map");
	        
	//CLonage de Yinf
	        
		  for (int l=0;l<Yinf.size();l++) {
			  image.Yinf.add(Yinf.get(l));
		  }
		  
	      return image;
	   }
	
	private static void printTab(double[] t) {
		int m = t.length;
		for (int i =0;i<m;i++) {
			System.out.print(" " +t[i] +", ");
		}
	}
	private static void printTab(int[] t) {
		int m = t.length;
		for (int i =0;i<m;i++) {
			System.out.print(" " +t[i] +", ");
		}
	}
	
	
	public void printHash() {
		System.out.println("Observation de la hash map");
        for (Map.Entry mapentry : coordinates.entrySet()) {
	           double key = (double) mapentry.getKey(); //est ce que ca marche ?
	           System.out.println("Valeur Y");
	           System.out.println(key);
	           int[] t0 = coordinates.get(key);
	           int i = t0[0];
	           int k = t0[1];
	           System.out.println("Arete : " +i +" Indice : " + k);
     }
	}
	
}
	