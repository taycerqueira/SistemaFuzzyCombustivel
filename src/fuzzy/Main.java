package fuzzy;

import wangmendel.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import net.sourceforge.jFuzzyLogic.*;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

public class Main {
	
	static String nomeBase = "autoMPG8-5-1tra";
	static DataSource source;
	static int quantConjuntosFuzzy = 3;
	static double porcentagemTeste = 0.2;
	
	public static void main(String[] args) throws Exception {
		
		source = new DataSource ("bases/arff/" + nomeBase + ".arff");
	    Instances D = source.getDataSet();
	    //imprime informações associadas à base de dados
	    int numInstancias = D.numInstances();
	    System.out.println("* Quantidade de instâncias: " + numInstancias);  
	    System.out.println("* Quantidade de atributos: " + D.numAttributes());
	    System.out.println("* Quantidade de conjuntos fuzzy por atributo: " + quantConjuntosFuzzy);
		
	    int quantidade = (int) (numInstancias*porcentagemTeste);
	    System.out.println("Quantidade de instâncias de teste: " + quantidade);
	    
		//Separo as instâncias que irão ser utilizadas para teste
	    //gerarInidicesTeste(quantidade, numInstancias);
	    
	    //Pego as instancias de teste a partir de um arquivo 
		//int[] indicesTeste = getIndicesTeste(quantidade);
		
		//Gera as regras usando wang-mendel e gera o arquivo .fcl
		wangMendel();
		
		//Cria o sistema fuzzy a partir do arquivo fcl
		FIS fis = FIS.load(("bases/fcl/" + nomeBase+".fcl"), true);

		if (fis == null) {
			System.err.println("Can't load file: '" + nomeBase + "'");
			System.exit(1);
		}
		
		System.out.println("\n=> Gerando sistema fuzzy...");
		FunctionBlock fb = fis.getFunctionBlock(null);
		//JFuzzyChart.get().chart(fb);

		// Set inputs
		fb.setVariable("Cylinders", 4);
		fb.setVariable("Displacement", 112);
		fb.setVariable("Horse_power", 88);
		fb.setVariable("Weight", 2605);
		fb.setVariable("Acceleration", 19.6);
		fb.setVariable("Model_year", 82);
		fb.setVariable("Origin", 1);

		// Evaluate
		fb.evaluate();

		// Show output variable's chart
		fb.getVariable("Mpg").defuzzify();
		System.out.println("OUTPUT (Mpg): " + fb.getVariable("Mpg").getValue());
		
		 // Show each rule (and degree of support)
	    //for(Rule r : fb.getFuzzyRuleBlock("No1").getRules())
	      //System.out.println(r);
		
	}
	
	public static void wangMendel(){
		
		long inicio = System.currentTimeMillis(); 
	    
		try {
		    
		    System.out.println("=> Executando algoritmo de Wang-Mendel. Aguarde...");
		    WangMendel wm = new WangMendel(source, quantConjuntosFuzzy);
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
	
	private static boolean isParaTeste(int k, int[] indicesTeste){
		
		boolean resultado = false;
		for(int i = 0; i < indicesTeste.length; i++){
			if(indicesTeste[i] == k){
				resultado =  true;
				break;
			}
		}
		return resultado;
	}
	
	private static int[] sortearInstanciasTeste(int quantidade, int numInstancias){
		
		int[] indices = new int[quantidade];
		
		List<Integer> numeros = new ArrayList<Integer>();
		for (int i = 0; i < numInstancias; i++) { 
		    numeros.add(i);
		}
		//Embaralhamos os números:
		Collections.shuffle(numeros);
		//Adicionamos os números aleatórios no vetor
		for (int i = 0; i < quantidade; i++) {
			indices[i] = numeros.get(i);
		}
		
		return indices;
		
	}
	
	private static void gerarArquivoFcl(ArrayList<Regra> regras, ArrayList<Atributo> listaAtributos) throws IOException{
		
	    System.out.println("=> Gerando arquivo .fcl...");
	    
	    //Gerar arquivo contendo o RULEBLOCK para ser inserido em um arquivo .fcl
	    FileWriter arquivo = new FileWriter("bases/fcl/" + nomeBase + ".fcl"); 
	    PrintWriter texto = new PrintWriter(arquivo);
	    
	    int indiceOutput = listaAtributos.size() - 1;
	    Atributo consequente = listaAtributos.get(indiceOutput);
	    
	    texto.println("FUNCTION_BLOCK " + nomeBase.replace("-", ""));
	    
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
	    	//Os pontos dos conjuntos fuzzy são de pertinência triangular (3 pontos por conjunto)
	    	for (ConjuntoFuzzy conjuntoFuzzy : atributo.getConjuntosFuzzy()) {	
	    		texto.print("\t TERM ");
	    		texto.print(conjuntoFuzzy.getIdConjunto());
	    		texto.print(" := ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
	    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
	    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
			}
	    	texto.println("END_FUZZIFY\n");
	    
	    }
	    
	    texto.println("DEFUZZIFY " + consequente.getNome());
    	for (ConjuntoFuzzy conjuntoFuzzy : consequente.getConjuntosFuzzy()) {	
    		texto.print("\t TERM ");
    		texto.print(conjuntoFuzzy.getIdConjunto());
    		texto.print(" := ");
    		texto.print("(" + conjuntoFuzzy.getLimiteInferior() + ", 0) ");
    		texto.print("(" + conjuntoFuzzy.getM() + ", 1) ");
    		texto.print("(" + conjuntoFuzzy.getLimiteSuperior() + ", 0);\n");
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
	
	//Gera os indices de teste e coloca em um arquivo
	public static void gerarInidicesTeste(int quantidade, int numInstancias){
		
		int[] indicesTeste = sortearInstanciasTeste(quantidade, numInstancias);
		
		File arquivo = new File("indicesTeste.txt");
		
		try(FileWriter fw = new FileWriter(arquivo)){
			for (int indice : indicesTeste) {
				fw.write(indice + "\r\n");
			}
		    fw.flush();
		}catch(IOException ex){
		  ex.printStackTrace();
		}
		
	}
	
	//Lê o arquivo e retorna os indices de teste
	public static int[] getIndicesTeste(int quantidade){
		
		int[] indicesTeste = new int[quantidade];
		int i = 0;
		
		File arquivo = new File("indicesTeste.txt");
		try(InputStream in = new FileInputStream(arquivo) ){
		  Scanner scan = new Scanner(in);
		  while(scan.hasNext()){
		    String indice = scan.nextLine();
		    if(indice.length() > 0){
		    	indicesTeste[i] = Integer.parseInt(indice);
		    	i++;
		    }
		  }
		}catch(IOException ex){
		  ex.printStackTrace();
		}
		
		return indicesTeste;
		
	}

}
