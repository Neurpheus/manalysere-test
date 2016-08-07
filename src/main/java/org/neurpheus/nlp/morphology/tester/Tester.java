/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.neurpheus.nlp.morphology.tester;

import com.dawidweiss.morfeusz.Analyzer;
import com.dawidweiss.morfeusz.InterpMorf;
import com.dawidweiss.morfeusz.Morfeusz;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Locale;
import java.util.StringTokenizer;
import morfologik.stemmers.Lametyzator;
import morfologik.stemmers.Stempel;
import morfologik.stemmers.Stempelator;
import org.apache.log4j.Logger;
import org.neurpheus.collections.tree.linkedlist.LinkedListTreeNode;
import org.neurpheus.nlp.morphology.DefaultMorphologyFactory;
import org.neurpheus.nlp.morphology.MorphologicalAnalyser;
import org.neurpheus.nlp.morphology.MorphologicalAnalysisResult;
import org.neurpheus.nlp.morphology.impl.MorphologicalAnalyserImpl;

/**
 *
 * @author jstrychowski
 */
public class Tester {
    
    private static Logger logger = Logger.getLogger(Tester.class);

    public static final int NAIVE = 0;
    public static final int STEMPEL = 1;
    public static final int LEMATYZATOR = 2;
    public static final int STEMPELATOR = 3;
    public static final int MORFEUSZ = 4;
    public static final int NEURPHEUS = 5;
    public static final int NEURPHEUS_80 = 6;
    
    public static String DELIMETERS = " \t\r\n!?.,;:'\"/\\|[]{}()+=!@#$%^&*~`<>-„”\uFEFF";
    public static String SENTENCE_END_DELIMETERS = "\r\n!?.!\uFEFF";
    

    public static Stempelator stempelator = null;;
    public static Stempel stempel = null;
    public static Lametyzator lematyzator = null;
    public static Analyzer morfeusz = null;
    public static MorphologicalAnalyser neurpheus = null;
    

    public static boolean USE_NEURALNET = true;
    public static boolean USE_DICT = true;

    private static Locale locale = new Locale("pl", "PL");
    //private static Locale locale = new Locale("ru", "RU");
    //private static Locale locale = new Locale("cs", "CZ");
    //private static Locale locale = new Locale("fr", "FR");
    
    private static final String TEST_TEXT = 
            "to jest przykładowy tekst do testów startu komponentu." +
            " Ala ma kota na punkcie psa." +
            " ile masz lat " +
            " Litwo ojczyzno moja ty jesteś jak zdrowię." +
            " ilę cię trzeba cenić ten tylko się dowie kto cię stracił.";
    
    public static void initComponent(int componentType) throws Exception {
        long startTime = System.currentTimeMillis();
        switch (componentType) {
            case STEMPEL:
                stempel = new Stempel();
                break;
            case LEMATYZATOR:
                lematyzator = new Lametyzator();
                break;
            case STEMPELATOR:
                stempelator = new Stempelator();
                break;
            case MORFEUSZ:
                morfeusz = Morfeusz.getInstance().getAnalyzer();
                break;
            case NEURPHEUS:
            case NEURPHEUS_80:
                neurpheus = DefaultMorphologyFactory.getInstance().getMostAccurateMorphologicalAnalyser(locale);
                neurpheus.init();
                ((MorphologicalAnalyserImpl) neurpheus).setUseNeuralNetwork(USE_NEURALNET);
                ((MorphologicalAnalyserImpl) neurpheus).setUseBaseFormsDictionary(USE_DICT);
                break;
            case NAIVE:
                break;
        }
//        StringTokenizer tokenizer = new StringTokenizer(TEST_TEXT, DELIMETERS, true);
//        tokenizer.hasMoreElements();
//        while (tokenizer.hasMoreTokens()) {
//            String token = tokenizer.nextToken();
//            if (token.trim().length() > 0) {
//                lemmatize(componentType, token);
//            }
//        }
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Initialization time " + duration + " ms for component " + componentType);
    }

    
    public static void produceTestFile(String inputDir, String outputPath) throws Exception {
        initComponent(STEMPELATOR);
        initComponent(MORFEUSZ);
        initComponent(NEURPHEUS);
        
        File dir = new File(inputDir);
        File[] files = dir.listFiles();
        BufferedWriter writer  = new BufferedWriter(new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(outputPath)), "UTF-8"));
        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            BufferedReader reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(f)), "UTF-8"));
            String line;
            boolean endOfSentence = true;
            do {
                line = reader.readLine();
                if (line != null) {
                    StringTokenizer tokenizer = new StringTokenizer(line, DELIMETERS, true);
                    tokenizer.hasMoreElements();
                    while (tokenizer.hasMoreTokens()) {
                        String token = tokenizer.nextToken();
                        if (token.trim().length() > 0) {
                            if (DELIMETERS.indexOf(token) >= 0) {
                                if (SENTENCE_END_DELIMETERS.indexOf(token) >= 0) {
                                    endOfSentence = true;
                                }
                            } else {
                                char firstChar = token.charAt(0);
                                boolean startsFromUppercase = Character.isUpperCase(firstChar);
                                boolean endsWithUppercase = Character.isUpperCase(token.charAt(token.length() -1 ));
                                boolean isUppercased = startsFromUppercase && endsWithUppercase && token.equals(token.toUpperCase());
                                if (!isUppercased && startsFromUppercase && endOfSentence) {
                                    token = token.toLowerCase();
                                }
                                String lemma = "xxx";
                                String lemma1 = neurpheus.getLemma(token);
                                String lemma2 = null;
                                String[] results = stempelator.stem(token);
                                if (results != null && results.length > 0) {
                                    lemma2 = results[0];
                                }
                                if (lemma2 != null && lemma1.equals(lemma2)) {
                                    lemma = lemma1;
                                } else {
                                    String lemma3 = null;
                                    InterpMorf[] mr = morfeusz.analyze(token);
                                    if (mr != null && mr.length > 0) {
                                        lemma3 = mr[0].getLemmaImage();
                                    }
                                    if (lemma3 != null) {
                                        if (lemma1.equals(lemma3)) {
                                            lemma = lemma3;
                                        } else if (lemma2 != null && lemma2.equals(lemma3)) {
                                            lemma = lemma2;
                                        }
                                    }
                                }
                                StringBuffer buffer = new StringBuffer();
                                buffer.append(lemma);
                                buffer.append('\t');
                                buffer.append(token);
                                String s = buffer.toString();
                                writer.write(s);
                                writer.newLine();
                                System.out.println(s);
                                endOfSentence = false;
                            }
                        }
                    }
                }
                
            } while (line != null);
            reader.close();
        }
        writer.close();
        
//                List result = Collections.EMPTY_LIST;
//                try {
//                    result = analyser.analyse2list(token);
//                } catch (MorphologyException ex) {
//                    Logger.getLogger(MorphologyTestFrame.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                if (result.size() > 0) {
//                    boolean show = true;
//                    if (jShowOnlyUncertainResults.isSelected()) {
//                        MorphologicalAnalysisResult mres = (MorphologicalAnalysisResult) result.get(0);
//                        show = !mres.isCertain();
//                    }
//                    if (show) {
//                        output.append(token);
//                        output.append(" :");
//                        for (final Iterator it = result.iterator(); it.hasNext();) {
//                            MorphologicalAnalysisResult mres = (MorphologicalAnalysisResult) it.next();
//                            String mrestxt = mres.toString();
//                            output.append("\r\n ");
//                            output.append(mrestxt);
//                        }
//                        output.append("\r\n\r\n");
//                    }
//                }
        
    }

    public static String[] singleResult = new String[1];

    public static String[] analyse(int componentType, String form) throws Exception {
        String[] result = singleResult;
        switch (componentType) {
            case MORFEUSZ:
                InterpMorf[] morfeuszResult = morfeusz.analyze(form);
                int count = morfeusz.getTokensNumber();
                result = new String[count];
                for (int i = 0; i < count; i++) {
                    result[i] = morfeuszResult[i].getLemmaImage() + "[" + morfeuszResult[i].toString() + "]";
                }
                break;
            case NEURPHEUS:
                //result = neurpheus.getLemmas(form);
                MorphologicalAnalysisResult[] nr = neurpheus.analyse(form);
                int i = 1;
                while (i < nr.length && nr[i].getAccuracy() == 1.0) {
                    i++;
                }
                result = new String[i];
                for (int j = 0; j < i; j++) {
                    result[j] = nr[j].toString();
                }
                break;
            case NAIVE:
                singleResult[0] = form;
                break;
        }
        return result;
    }
    
    public static String[] lemmatize(int componentType, String form) throws Exception {
        String[] result = singleResult;
        switch (componentType) {
            case STEMPEL:
                result = stempel.stem(form);
                break;
            case LEMATYZATOR:
                result = lematyzator.stem(form);
                break;
            case STEMPELATOR:
                result = stempelator.stem(form);
                break;
            case MORFEUSZ:
                InterpMorf[] morfeuszResult = morfeusz.analyze(form);
                int count = morfeusz.getTokensNumber();
                result = new String[count];
                for (int i = 0; i < count; i++) {
                    result[i] = morfeuszResult[i].getLemmaImage();
                }
                break;
            case NEURPHEUS:
                //result = neurpheus.getLemmas(form);
                MorphologicalAnalysisResult[] nr = neurpheus.analyse(form);
                int i = 1;
                while (i < nr.length && nr[i].getAccuracy() == 1.0) {
                    i++;
                }
                result = new String[i];
                for (int j = 0; j < i; j++) {
                    result[j] = nr[j].getForm();
                }
                break;
            case NEURPHEUS_80:
                //result = neurpheus.getLemmas(form);
                nr = neurpheus.analyse(form);
                double bestResult = 0.8 * nr[0].getAccuracy();
                i = 1;
                while (i < nr.length && nr[i].getAccuracy() >= bestResult) {
                    i++;
                }
                result = new String[i];
                for (int j = 0; j < i; j++) {
                    result[j] = nr[j].getForm();
                }
                break;
            case NAIVE:
                singleResult[0] = form;
                break;
        }
        return result;
        
    }
    
    public static void testAnalyser(
            String inPath, int componentType, 
            boolean doNotTestFitstColumn, boolean displayErrors) throws Exception {
        BufferedReader reader = null; 
        reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(new FileInputStream(inPath)), "UTF-8"));
        String line;
        int allFormsCount = 0;
        int correctFormsCount = 0;
        int correctForms2Count = 0;
        int correctBaseFormsCount = 0;
        int baseFormCount = 0;
        int errorsCount = 0;
        int number = 0;
        int numberOfResults = 0;
        final int numberOfLinesBetweenInfoMessages = 1000;

        if (componentType == NEURPHEUS || componentType == NEURPHEUS_80)  {
            ((MorphologicalAnalyserImpl) neurpheus).clearCache();
        }

        String[] result = null;
        InterpMorf[] morfeuszResult = null;
        long startTime = System.currentTimeMillis();
        try {
            do {
                line = reader.readLine();
                if (line != null && line.trim().length() > 0) {
                    line = line.trim();
                    String[] tab = line.split("\\s");
                    String baseForm = tab[0];//.toLowerCase();
                    String baseForm2 = null;
                    int pos = baseForm.indexOf('|');
                    if (pos > 0) {
                        baseForm2 = baseForm.substring(pos + 1);
                        baseForm = baseForm.substring(0, pos);
                    }
                    for (int i = doNotTestFitstColumn ? 1 : 0; i < tab.length; i++) {
                        allFormsCount++;
                        String form = tab[i];//.toLowerCase();
                        boolean analysingBaseForm = form.equalsIgnoreCase(baseForm) || form.equalsIgnoreCase(baseForm2);
                        if (analysingBaseForm) {
                            baseFormCount++;
                        }
                        String resultForm = form;
                        numberOfResults++;
                        result = lemmatize(componentType, form);
                        if (result != null) {
                            resultForm = result[0];
                            if (result.length > 1) {
                                numberOfResults += result.length - 1;
                            }
                        }
                        if (resultForm.length() == 0) {
                            resultForm = form;
                        }
                        if (baseForm.equalsIgnoreCase(resultForm) || (baseForm2 != null && baseForm2.equalsIgnoreCase(resultForm))) {
                            correctFormsCount++;
                            correctForms2Count++;
                            if (analysingBaseForm) {
                                correctBaseFormsCount++;
                            }
                        } else {
                            boolean isError = true;
                            StringBuffer otherResults = new StringBuffer();
                            otherResults.append("  other results: ");
                            if (result != null) {
                                for (int j = 1; j < result.length; j++) {
                                    otherResults.append(' ');
                                    String lemma = result[j];
                                    otherResults.append(lemma);
                                    if (baseForm.equalsIgnoreCase(lemma) || (baseForm2 != null && baseForm2.equalsIgnoreCase(lemma))) {
                                        isError = false;
                                    }
                                }
                            }
                            if (isError) {
                                errorsCount++;
                                if (displayErrors) {
                                    logger.info("Error during analysis:");
                                    logger.info("  analysed form: " + form);
                                    logger.info("  expected form: " + baseForm);
                                    logger.info("  result form: " + resultForm);
                                    logger.info(otherResults.toString());
                                }
                            } else {
                                correctForms2Count++;
                            }
                            
                        }
                    }
                    number++;
                    if (number % numberOfLinesBetweenInfoMessages == 0) {
                        logger.info("Numer of processed forms: " + allFormsCount);
                        StringBuffer info = new StringBuffer();
                        if (allFormsCount > 0) {
                            info.append(" Accuracy(first): ");
                            info.append(100.0 * ((double) correctFormsCount) / allFormsCount);
                            info.append('%');
                            info.append(" Accuracy(all): ");
                            info.append(100.0 * ((double) correctForms2Count) / allFormsCount);
                            info.append('%');
                            info.append(" avg.results: ");
                            info.append(((double) numberOfResults) / allFormsCount);
                        }
                        if (baseFormCount > 0) {
                            info.append("  accuracy(lemma): ");
                            info.append(100.0 * ((double) correctBaseFormsCount) / baseFormCount);
                            info.append('%');
                        }
                        long duration = System.currentTimeMillis() - startTime;
                        if (duration > 0) {
                            double speed = 1000.0 * allFormsCount / duration;
                            info.append("   Speed: ");
                            info.append(speed);
                            info.append(" forms/s");
                        }
                        logger.info(info.toString());
                    }
                }
            } while (line != null);
            logger.info("****************** TEST RESULTS ****************");
            if (allFormsCount > 0) {
                logger.info(" Accurracy: " + 100.0 * ((double) correctFormsCount) / allFormsCount + "%");
                logger.info(" Accurracy considering all results : " + 100.0 * ((double) correctForms2Count) / allFormsCount + "%");
                logger.info(" Number of results per analyse : " + ((double) numberOfResults) / allFormsCount);
            }
            if (baseFormCount > 0) {
                logger.info(" Base forms accurracy: " + 100.0 * ((double) correctBaseFormsCount) / baseFormCount + "%");
            }
            logger.info(" Number of errors : " + errorsCount);
            if (errorsCount > 0) {
                logger.info(" Number of correct results per wrong result : " + allFormsCount / errorsCount);
            }
            long duration = System.currentTimeMillis() - startTime;
            if (duration > 0) {
                double speed = 1000.0 * allFormsCount / duration;
                logger.info("Speed = " + speed);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.info("Cannot properly close input stream.", e);
                }
            }
        }
    }

    public static void analyse(int componentType, String[] forms) throws Exception {
        for (int i = 0; i < forms.length; i++) {
            String form = forms[i];
            String[] results = analyse(componentType, form);
            System.out.println(form);
            for (int j = 0; j < results.length; j++) {
                System.out.print("  ");
                System.out.println(results[j]);
            }
        }
    }

    public final static String[] PRONOUNS =
    {
// czasowniki
"być", "jestem", "jesteś", "jest", "jesteśmy", "jesteście", "są", "byłem", "byłeś", "był", "była", "było", "byliśmy", "byliście", "byli", "były", "będę", "będziesz", "będzie", "będziemy", "będziecie", "będą", "bądź", "bądźmy", "bądźcie", "byłbym", "byłabym", "byłobym", "bylibyście", "byłybyście", "byliby", "byłyby", "byłoby", "byłbyś", "byłby", "byłaby", "byliby", "byłyby", "będący", "będąc",
"mieć", "mam", "masz", "ma", "mamy", "macie", "mają", "miałem", "miałam", "miałeś", "miałaś", "miał", "miała", "miało", "mieliśmy", "mieliście", "mieli", "miały", "miałbym", "miałabym", "miałobym", "miałbyś", "miałabyś", "miałobyś", "miałby", "miałaby", "miałoby", "mielibyśmy", "miałybyśmy", "mielibyście", "miałybyście", "mieliby", "miałyby", "miej", "miejmy", "miejcie", "mający", "mając",

//partykuły
"nie",
"ani",
"razy",
"tylko",
"by",
"czy",
"niech", "niechaj",
"bodaj",
"no",
"oby",

// partykuła i zaimek
"tak",

//", "zaimki", "osobowe
"ja", "mnie", "mi", "mną", "mnie",
"ty", "ciebie", "cię", "tobie", "ci", "tobą", "tobie",
"on", "ona", "ono", "jego", "go", "niego", "jej", "niej", "jemu", "mu", "niemu", "jemu", "mu", "niemu", "ją", "nią", "je", "nie", "nim", "nią", "niej", "oni", "one", "ich", "nich", "im", "nim", "je", "nie", "nimi",
"my", "nas", "nam", "nami", "",
"wy", "was", "wam", "wami",

//", "zaimki", "dzierżawcze
"mój", "mojego", "mego", "mojemu", "memu", "mego", "moim", "mym", "moja", "ma", "mojej", "mej", "moją", "mą", "moje", "me", "mojego", "mego", "mojemu", "memu", "moim", "mym", "moi", "moich", "mych", "moim", "mym", "moimi", "mymi", "moje", "me", "moich", "mych", "moim", "mym", "moimi", "mymi",
"twój", "twojego", "twego", "twojemu", "twemu", "twoim", "twym", "twoja", "twa", "twojej", "twej", "twoją", "twą", "twoje", "twe", "twojego", "twego", "twojemu", "twemu", "twoim", "twym", "twoi", "twoich", "twych", "twoim", "twym", "twoimi", "twymi", "twoje", "twe", "twoich", "twych", "twoim", "twym", "twoimi", "twymi",
"swój", "swojego", "swego", "swojemu", "swemu", "swoim", "swym", "swoja", "swa", "swojej", "swej", "swoją", "swą", "swoje", "swe", "swojego", "swego", "swojemu", "swemu", "swoim", "swym", "swoi", "swoich", "swych", "swoim", "swym", "swoimi", "swymi", "swoje", "swe", "swoich", "swych", "swoim", "swym", "swoimi", "swymi",

"nasz", "naszego", "naszemu", "naszym", "nasza", "naszej", "naszą", "nasze", "naszego", "naszemu", "naszym", "nasi", "naszych", "naszym", "naszymi", "nasze", "naszych", "naszym", "naszymi", 
"wasz", "waszego", "waszemu", "waszym", "wasza", "waszej", "waszą", "wasze", "waszego", "waszemu", "waszym", "wasi", "waszych", "waszym", "waszymi", "wasze", "waszych", "waszym", "waszymi",

"czyj", "czyjego", "czyjemu", "czyim", "czyja", "czyjej", "czyją", "czyje", "czyjego", "czyjemu", "czyim", "czyi", "czyich", "czyim", "czyimi", "czyje", "czyich", "czyim", "czyimi", 
"niczyj", "niczyjego", "niczyjemu", "niczyim", "niczyja", "niczyjej", "niczyją", "niczyje", "niczyjego", "niczyjemu", "niczyim", "niczyi", "niczyich", "niczyim", "niczyimi", "niczyje", "niczyich", "niczyim", "niczyimi", 
"czyjś", "czyjegoś", "czyjemuś", "czyimś", "czyjaś", "czyjejś", "czyjąś", "czyjeś", "czyjegoś", "czyjemuś", "czyimś", "czyiś", "czyichś", "czyimś", "czyimiś", "czyjeś", "czyichś", "czyimś", "czyimiś",
"czyjkolwiek", "czyjegokolwiek", "czyjemukolwiek", "czyimkolwiek", "czyjakolwiek", "czyjejkolwiek", "czyjąkolwiek", "czyjekolwiek", "czyjegokolwiek", "czyjemukolwiek", "czyimkolwiek", "czyikolwiek", "czyichkolwiek", "czyimkolwiek", "czyimikolwiek", "czyjekolwiek", "czyichkolwiek", "czyimkolwiek", "czyimikolwiek",


//", "zaimki", "zwrotne
"się", "siebie", "sobie",

//", "zaimki", "określone
"ten", "tego", "temu", "tym", "ta", "tej", "tę", "tą", "to", "tego", "temu", "tym", "ci", "tych", "tym", "tymi", "te", "tych", "tym", "te", "tymi",
"tamten", "tamtego", "tamtemu", "tamtym", "tamta", "tamtej", "tamtę", "tamtą", "tamto", "tamtego", "tamtemu", "tamtym", "tamci", "tamtych", "tamtym", "tamtymi", "tamte", "tamtych", "tamtym", "tamte", "tamtymi",
"ów", "owego", "owemu", "owym", "owa", "owej", "ową", "owej", "owo", "owego", "owemu", "owym", "owi", "owych", "owym", "owymi", "owe", "owych", "owym", "owe", "owymi", 
"taki", "takiego", "takiemu", "takim", "taka", "takiej", "taką", "takie", "takiego", "takiemu", "takim", "tacy", "takich", "takim", "takimi", "takie", "takich", "takim", "takimi",
"tu",
"tam",
"stąd",
"stamtąd",
"tędy",
"wtedy",

//", "zaimki", "pytające
"kto", "kogo", "komu", "kim",
"co", "czego", "czemu", "czym",

        
    };
    
    
    public static void produceTestFile() {
        String inPath = "n:/manalyser-tester/test-data/input-files";
        String outPath = "n:/manalyser-tester/test-data/test-file.txt";
        try {
            produceTestFile(inPath, outPath);
        } catch (Exception ex) {
            logger.error("Cannot produce test file", ex);
        }
    }

    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        
//        produceTestFile();

        
        
//        String inPath = "n:/manalyser-tester/test-data/input-files";
//        String inPath = "n:/manalyser-tester/test-data/pl_words_freq_totest.txt";
//        String inPath = "N:/manalyser-tester/test-data/wikipedia_pl_top_words.txt";
//        String inPath = "n:/manalyser-tester/test-data/wikipedia-top-test.txt";
//        String inPath = "n:/manalyser-tester/test-data/test-file1.txt";
//        String outPath = "n:/manalyser-tester/test-data/test-file1.txt";

        String inPath = "n:/data/dictionaries/full_pl_PL.all";
//        String inPath = "n:/data/dictionaries/cs_CZ.all";
//        String inPath = "n:/data/dictionaries/ru_RU.all";
//        String inPath = "n:/data/dictionaries/fr_FR.all";
//        String inPath = "n:/manalyser-tester/test-data/medycyna-totest.txt";
        
        boolean doNotTestFirtColumn = false;
        
        
        boolean displayErrors = false;

        USE_NEURALNET = false;
        USE_DICT = true;
        
        int iterations = 0;
        
        try {
            
            int componentType = NAIVE;
            componentType = STEMPEL;
            //componentType = LEMATYZATOR;
            //componentType = STEMPELATOR;
            //componentType = MORFEUSZ;
            componentType = NEURPHEUS;
            //componentType = NEURPHEUS_80;

            initComponent(componentType);
            //LinkedListTreeNode.OLD_SOLUTION = true;
            LinkedListTreeNode.OLD_SOLUTION = false;

            for (int i = 0; i < iterations; i++) {
                testAnalyser(inPath, componentType, doNotTestFirtColumn, displayErrors);
            }
        
            //analyse(componentType, new String[] {"niebiorący"});
            //analyse(componentType, new String[] {"apostrofy"});
            //analyse(componentType, new String[] {"alkowami"});
        
//            produceTestFile(inPath, outPath);
//            produceTestFile("n:/manalyser-tester/test-data/medycyna", "n:/manalyser-tester/test-data/medycyna-totest.txt");

            //((MorphologicalAnalyserImpl) neurpheus).setIpb(null);
            //((MorphologicalAnalyserImpl) neurpheus).setNeuralNetwork(null);
            //((MorphologicalAnalyserImpl) neurpheus).setTree(null);
                    
            //neurpheus = null;
            for (int i = 0; i < 10; i++) {
                logger.info("Used memory : " + Math.round((Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024.0) + "kb");
                Thread.sleep(2000);
                System.gc();
                Thread.yield();
            }
//            lemmatize(componentType, "testowanie");
            
        } catch (Exception e) {
            logger.error("Error while testing", e);        
        }
    }


}
