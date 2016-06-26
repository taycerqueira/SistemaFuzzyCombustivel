package wangmendel;

import java.util.List;

public class Regra {
	
	private List<ConjuntoFuzzy> antecedentes;
	private ConjuntoFuzzy consequente;
	private double grau;
	
	public Regra(List<ConjuntoFuzzy> antecedentes, ConjuntoFuzzy consequente, double grau) {
		
		this.antecedentes = antecedentes;
		this.consequente = consequente;
		this.grau = grau;
		
	}
	
	public List<ConjuntoFuzzy> getAntecedentes() {
		return antecedentes;
	}

	public ConjuntoFuzzy getConsequente() {
		return consequente;
	}

	public double getGrau() {
		return grau;
	}
	
	public void imprimeRegra(){
		for (ConjuntoFuzzy conjuntoFuzzy : antecedentes) {
			System.out.print(conjuntoFuzzy.getIdConjunto() + " AND ");
		}
		System.out.print(" => " + consequente.getIdConjunto() + "\r\n");
	}

}
