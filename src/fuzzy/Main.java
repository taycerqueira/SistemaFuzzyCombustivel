package fuzzy;

import wangmendel.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import net.sourceforge.jFuzzyLogic.*;
import net.sourceforge.jFuzzyLogic.plot.JFuzzyChart;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Main {
	
	static final String nomeBaseTreinamento = "autoMPG8-5-1tra";
	static final String nomeBaseTeste = "autoMPG8-5-1tst";
	static final int quantConjuntosFuzzy = 3;
	
	static DataSource sourceTreinamento;
	static DataSource sourceTeste;
	
	public static void main(String[] args) throws Exception {
		
		sourceTreinamento = new DataSource ("bases/arff/" + nomeBaseTreinamento + ".arff");
		sourceTeste = new DataSource ("bases/arff/" + nomeBaseTeste + ".arff");
		
	    Instances dataTreinamento = sourceTreinamento.getDataSet();
	    Instances dataTeste = sourceTeste.getDataSet();
	    
	    //imprime informações associadas à base de dados
	    System.out.println("\n---------- SISTEMA FUZZY - ANÁLISE DE CONSUMO DE COMBUSTÍVEL ----------\n");
	    System.out.println("* Base de treinamento: " + nomeBaseTreinamento);
	    System.out.println("* Base de teste: " + nomeBaseTeste);
	    int numInstanciasTreinamento = dataTreinamento.numInstances();
	    System.out.println("* Quantidade de instâncias: " + numInstanciasTreinamento);  
	    System.out.println("* Quantidade de atributos: " + dataTreinamento.numAttributes());
	    System.out.println("* Quantidade de conjuntos fuzzy: " + quantConjuntosFuzzy);
	   
		//Gera as regras usando wang-mendel e gera o arquivo .fcl
		wangMendel(quantConjuntosFuzzy);
		
		//TESTANDO SISTEMA
		FIS fis = FIS.load(("bases/fcl/" + nomeBaseTreinamento+".fcl"), true);
		if (fis == null) {
			System.err.println("Can't load file: '" + nomeBaseTreinamento + "'");
			System.exit(1);
		}
		JFuzzyChart.get().chart(fis);

	    int numInstanciasTeste = dataTeste.numInstances();
	    System.out.println("* Quantidade de instâncias de teste: " + numInstanciasTeste);  
	    testarSistema(fis, sourceTeste);
		
	}
	
	public static void wangMendel(int quantConjuntosFuzzy){
		
		long inicio = System.currentTimeMillis(); 
	    
		try {
		    
		    System.out.println("\n=> Executando algoritmo de Wang-Mendel. Aguarde...");
		    WangMendel wm = new WangMendel(sourceTreinamento, sourceTeste, quantConjuntosFuzzy);
		    ArrayList<Regra> regras = wm.gerarRegras();
		    
		    System.out.println("Quantidade de regras geradas: " + regras.size());
		    
		    long fim  = System.currentTimeMillis();  
		    System.out.println("* Tempo de execução (min/seg): " + new SimpleDateFormat("mm:ss").format(new Date(fim - inicio)));
		    
		    //Gera arquivo .fcl (fuzzy control language)
		    gerarArquivoFcl(regras, wm.getListaAtributos());   
		    
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	private static void gerarArquivoFcl(ArrayList<Regra> regras, ArrayList<Atributo> listaAtributos) throws IOException{
		
	    System.out.println("\n=> Gerando arquivo .fcl...");
	    
	    //Gerar arquivo contendo o RULEBLOCK para ser inserido em um arquivo .fcl
	    FileWriter arquivo = new FileWriter("bases/fcl/" + nomeBaseTreinamento + ".fcl"); 
	    PrintWriter texto = new PrintWriter(arquivo);
	    
	    int indiceOutput = listaAtributos.size() - 1;
	    Atributo consequente = listaAtributos.get(indiceOutput);
	    
	    texto.println("FUNCTION_BLOCK " + nomeBaseTreinamento.replace("-", ""));
	    
	    texto.println("VAR_INPUT");  
	    for(int i= 0; i < indiceOutput; i++){
	    	Atributo atributo = listaAtributos.get(i);
	    	
	    	//Atualmente a o JFuzzyLogic só dá suporte ao tipo REAL
	    	texto.print("\t" + atributo.getNome() + " : REAL;\n"); 
	    	
	    }
	    texto.println("END_VAR\n");
	    
	    texto.println("VAR_OUTPUT");
	    	//Atualmente a o JFuzzyLogic só dá suporte ao tipo REAL
	    	texto.print("\t" + consequente.getNome() + " : REAL;");
	    texto.println("\nEND_VAR\n");
	    
	    for(int i= 0; i < indiceOutput; i++){
	    	Atributo atributo = listaAtributos.get(i);
	    	
	    	texto.println("FUZZIFY " + atributo.getNome());
	    	
	    	for (int c = 0; c < atributo.getConjuntosFuzzy().size(); c++) {	
	    		ConjuntoFuzzy conjuntoFuzzy = atributo.getConjuntosFuzzy().get(c);
	    		if(c == 0){
		    		texto.print("\t TERM ");
		    		texto.print(conjuntoFuzzy.getIdConjunto());
		    		texto.print(" := ");
		    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
		    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
	    		}
	    		else if(c == atributo.getConjuntosFuzzy().size() - 1){
		    		texto.print("\t TERM ");
		    		texto.print(conjuntoFuzzy.getIdConjunto());
		    		texto.print(" := ");
		    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
		    		texto.print("(" + conjuntoFuzzy.getM() + ", 1);\n");
	    		}
	    		else{
		    		texto.print("\t TERM ");
		    		texto.print(conjuntoFuzzy.getIdConjunto());
		    		texto.print(" := ");
		    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
		    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
		    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
	    		}

			}
	    	
	    	texto.println("END_FUZZIFY\n");
	    
	    }
	    
	    texto.println("DEFUZZIFY " + consequente.getNome());
    	
    	for (int c = 0; c < consequente.getConjuntosFuzzy().size(); c++) {	
    		ConjuntoFuzzy conjuntoFuzzy = consequente.getConjuntosFuzzy().get(c);
    		if(c == 0){
	    		texto.print("\t TERM ");
	    		texto.print(conjuntoFuzzy.getIdConjunto());
	    		texto.print(" := ");
	    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
    		}
    		else if(c == consequente.getConjuntosFuzzy().size() - 1){
	    		texto.print("\t TERM ");
	    		texto.print(conjuntoFuzzy.getIdConjunto());
	    		texto.print(" := ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
	    		texto.print("(" + conjuntoFuzzy.getM() + ", 1);\n");
    		}
    		else{
	    		texto.print("\t TERM ");
	    		texto.print(conjuntoFuzzy.getIdConjunto());
	    		texto.print(" := ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
	    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
    		}

		}
    	
	    texto.println("\t METHOD : COG;"); // Use 'Center Of Gravity' defuzzification method
	    texto.println("\t DEFAULT := 0;"); // Default value is 0 (if no rule activates defuzzifier)
	    texto.println("END_DEFUZZIFY\n");
	    
	    texto.println("RULEBLOCK No1");
	    texto.println("\t AND : MIN;"); //Use 'min' or 'PROD' for 'and'
	    texto.println("\t ACT : MIN;"); //Use 'min' activation method
	    //texto.println("\t ACT : PROD;"); //Use 'Product' activation method
	    texto.println("\t ACCU : MAX;\n"); //Use 'max' accumulation method
	    
	    //Regras
	    int contadorRegra = 1;
	    for (Regra regra : regras) {
	    	
	    	texto.print("\t RULE " + contadorRegra);
	    	texto.print(" : IF ");
	    	
	    	boolean primeiroConjunto = true;
	    	
	    	for (ConjuntoFuzzy conjunto : regra.getAntecedentes()) {
	    		
	    		if(primeiroConjunto){
	    			
		    		texto.print(conjunto.getNomeAtributo());
		    		texto.print(" IS ");
		    		texto.print(conjunto.getIdConjunto());
		    		
		    		primeiroConjunto = false;
	    			
	    		}
	    		else{
	    			
	    			texto.print(" AND ");
		    		texto.print(conjunto.getNomeAtributo());
		    		texto.print(" IS ");
		    		texto.print(conjunto.getIdConjunto());
	    			
	    		}
				
			}
	    	
	    	texto.print(" THEN ");
	    	texto.print(regra.getConsequente().getNomeAtributo());
	    	texto.print(" IS ");
	    	texto.print(regra.getConsequente().getIdConjunto());
	    	texto.print(";\n");
	    	
	    	contadorRegra++;
			
		}
	     
	    texto.println("END_RULEBLOCK\n");
	    
	    texto.println("END_FUNCTION_BLOCK");
	    
	    arquivo.close();
	    System.out.println("Arquivo .fcl gerado com sucesso.");
		
	}
	
	private static void testarSistema(FIS fis, DataSource sourceTeste) throws Exception{
		
		Instances instancias = sourceTeste.getDataSet();
		
		System.out.println("\n=> Testando sistema...");
		double somatorioErro = 0;
		double mse = 0;
		int tamanhoBaseTeste = sourceTeste.getDataSet().size();
		
		for (int k = 0; k < tamanhoBaseTeste; k++ ) {
			
			//System.out.println("\n - Instância " + k);
				
			//System.out.println("Número da instância: " + k);
			Instance instancia = instancias.get(k);
			//Seta as entradas
			//Considerando que o último atributo é sempre o atributo que corresponde a classe da instância, por isso usa-se o -1
			for (int i = 0; i < (instancias.numAttributes() - 1); i++) {
				
				if(instancias.attribute(i).isNumeric()){

					String nomeAtributo = instancias.attribute(i).name();
					double valor = instancia.value(i);
					//System.out.println(nomeAtributo + ": " + valor);
					
					fis.setVariable(nomeAtributo, valor);
				}	

			}
			
			// Evaluate
			fis.evaluate();
			
			//JFuzzyChart.get().chart(fis);
			
			double fuzzyOutput = fis.getVariable("Mpg").getValue();
			//System.out.println(" -- Saída fuzzy: " + fuzzyOutput);
			double realOutput = instancia.value(instancias.numAttributes() - 1);
			//System.out.println(" -- Saída esperada: " + realOutput);
			
			double erro = Math.pow((realOutput - fuzzyOutput), 2);
			//System.out.println(" --- Erro: " + erro);
			somatorioErro += erro;

		}
		
		mse = somatorioErro/tamanhoBaseTeste;
		System.out.println("\nMSE (Erro quadrático médio): " + mse);
		
	}

}
