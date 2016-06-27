package wangmendel;

import weka.core.converters.ConverterUtils.DataSource;
import weka.experiment.Stats;
import java.util.ArrayList;
import java.util.List;
import weka.core.AttributeStats;
import weka.core.Instance;
import weka.core.Instances;

public class WangMendel {
	
	private DataSource dadosTreinamento;
	private DataSource dadosTeste;
	private Instances instancias;
	private int quantRegioes;
	private ArrayList<Atributo> listaAtributos;
	
	
	public WangMendel(DataSource dadosTreinamento, DataSource dadosTeste, int quantRegioes) throws Exception{
		this.dadosTreinamento = dadosTreinamento;
		this.dadosTeste = dadosTeste;
		this.quantRegioes = quantRegioes;
		this.instancias = dadosTreinamento.getDataSet();
		gerarlistaAtributos();
	}
	
	public ArrayList<Regra> gerarRegras() throws Exception{
		
		ArrayList<Regra> regras = new ArrayList<Regra>();
		
		//Armazena o indice do atributo que corresponde a classe. Aqui considero que é sempre o último atributo.
		//int indiceConsequente = dados.getDataSet().numAttributes() - 1; 
		
		int contInstancias = 0; 
		for (int k = 0; k < instancias.size(); k++ ) {
			
			contInstancias++;
			Instance instancia = instancias.get(k);
			
			List<ConjuntoFuzzy> antecedentes = new ArrayList<ConjuntoFuzzy>();
			//double consequente = instancia.value(indiceConsequente);
			ConjuntoFuzzy consequente = null;
			
			double grauRegra = 1; //Começa com 1 por ser um fator neutro na multiplicação
			
			//Pega cada atributo da instância
			for (int i = 0; i < dadosTreinamento.getDataSet().numAttributes(); i++) {
				
				double valor = instancia.value(i);
				Atributo atributo  = getAtributoByName(instancia.attribute(i).name());
				
				//Para cada atributo da instância, verifico o conjunto fuzzy de maior grau
				double maiorGrau = Double.NEGATIVE_INFINITY;
				ConjuntoFuzzy conjuntoMaiorGrau = null;
				
				for (ConjuntoFuzzy conjunto : atributo.getConjuntosFuzzy()) {	
					double grau = conjunto.calculaPertinencia(valor);
					if(grau > maiorGrau){
						maiorGrau = grau;
						conjuntoMaiorGrau = conjunto;
					}
				}
				
				grauRegra *= maiorGrau;
				if(i == dadosTreinamento.getDataSet().numAttributes() - 1){
					consequente = conjuntoMaiorGrau;
				}
				else{
					
					//Adiciono o conjunto fuzzy de maior grau no antecedente da regra
					antecedentes.add(conjuntoMaiorGrau);
					
				}
					
			}
			
			//Verfico se já existe alguma regra com os mesmos antecedentes
			int quantAtributosAntecedentes = dadosTreinamento.getDataSet().numAttributes() - 1;
			
			if(regras.isEmpty()){
				Regra regra = new Regra(antecedentes, consequente, grauRegra);
				regras.add(regra);	
			}
			else{

				boolean jaExiste = false;
				for (Regra r1 : regras) {
					int cont = 0;
					for(ConjuntoFuzzy a2 : antecedentes){
						for(ConjuntoFuzzy a1 : r1.getAntecedentes()){
							if(a2.getIdConjunto().equals(a1.getIdConjunto())){
								cont++;
							}
						}
					}

					//Se todos os antecedentes forem iguais...
					if(cont == quantAtributosAntecedentes){
						jaExiste = true;
						//Verifico qual regra possui maior grau: a nova regra que estou tentando criar ou a que já existe na lista
						if(grauRegra > r1.getGrau()){
							regras.remove(r1);
							Regra regra = new Regra(antecedentes, consequente, grauRegra);
							regras.add(regra);
							break;

						}
					}
					
				}
				//Se não existe nenhuma regra igual a que estou tentando inserir, adiciono ela na lista
				if(!jaExiste){
					Regra regra = new Regra(antecedentes, consequente, grauRegra);
					regras.add(regra);
				}
				
			}
			
		}
		
		System.out.println("Quantidade de instâncias utilizadas no algoritmo: " + contInstancias);

		return regras;
		
	}
	
	private void gerarlistaAtributos() throws Exception{
		
		Instances instanciasTeste = dadosTeste.getDataSet();
		
		this.listaAtributos = new ArrayList<Atributo>();
		
		for (int i = 0; i < dadosTreinamento.getDataSet().numAttributes(); i++) {
			
			if(instancias.attribute(i).isNumeric()){
				
				AttributeStats asTreinamento = instancias.attributeStats(i);
				Stats sTreinamento = asTreinamento.numericStats;	
				
				AttributeStats asTeste = instanciasTeste.attributeStats(i);
				Stats sTeste = asTeste.numericStats;
				
				double min;
				double max;
				if(sTreinamento.min < sTeste.min){
					min = sTreinamento.min;
				}
				else{
					min = sTeste.min;
				}
				
				if(sTreinamento.max > sTeste.max){
					max = sTreinamento.max;
				}
				else{
					max = sTeste.max;
				}
				//System.out.println("Atributo: " + instancias.attribute(i).name());
				//System.out.println("Valor mínimo: " + s.min);
				//System.out.println("Valor máximo: " + s.max);
				
				Atributo atributo = new Atributo(instancias.attribute(i).name(), min, max, this.quantRegioes);
				listaAtributos.add(atributo);
				
			}	
		}

	}
	
	private Atributo getAtributoByName(String nomeAtributo){
		
		Atributo atributo = null;
		
		for (Atributo a : listaAtributos) {
			if(a.getNome().equals(nomeAtributo)){
				atributo = a;
				break;
			}
			
		}
		
		return atributo;
		
	}

	public ArrayList<Atributo> getListaAtributos() {
		return listaAtributos;
	}

}
