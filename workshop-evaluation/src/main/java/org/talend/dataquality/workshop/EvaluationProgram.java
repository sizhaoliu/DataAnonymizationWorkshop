package org.talend.dataquality.workshop;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

public class EvaluationProgram {

    private Map<String, String> secretMap = new HashMap<>();

    public EvaluationProgram(String passPhrase) throws IOException {
        loadSecretMap(passPhrase);
    }

    private void loadSecretMap(String passPhrase) throws IOException {
        try (InputStream in = EvaluationProgram.class.getResourceAsStream("reference_data.csv")) {
            List<String> lines = IOUtils.readLines(in, "UTF-8");
            secretMap = lines.stream().skip(1) // skip header line
                    .map(line -> AESToolkit.decrypt(passPhrase, line).split(","))
                    .collect(Collectors.toMap(a -> a[0] + " " + a[1], a -> a.length > 2 ? a[2] : "")); // concat first/last name
        }
    }

    private void runAllEvaluations() throws IOException {
        String answerParent = EvaluationProgram.class.getResource("Answers/").getPath();
        try (Stream<Path> paths = Files.walk(Paths.get(answerParent))) {
            // walk through the entire folder
            Map<String, Long> resultMap = paths.filter(Files::isRegularFile).map(path -> path.getFileName().toString())
                    .collect(Collectors.toMap(file -> file, file -> {
                        try {
                            return runEvaluationOnFile(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return 0L;
                    }));

            System.out.println("Ranking: ");
            resultMap.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .forEach(entry -> System.out.println(entry.getKey() + " got " + entry.getValue()
                            + " correct secrets out of " + secretMap.size()));
        }
    }

    private long runEvaluationOnFile(String fileName) throws IOException {
        try (InputStream in = EvaluationProgram.class.getResourceAsStream("Answers/" + fileName)) {
            List<String> lines = IOUtils.readLines(in, "UTF-8");
            Map<String, String> resultMap = lines.stream()
                    .filter(line -> !line.startsWith("#")) // omit lines starting with #
                    .limit(60) // validate the first 60 answers including the header
                    .map(line -> line.split("[,;]")) // split by comma or semicolon
                    .filter(array -> array.length > 2) // omit lines containing less than 3 tokens
                    .collect(Collectors.toMap(a -> a[0] + " " + a[1], a -> a[2]));

            return resultMap.entrySet().stream().filter(e -> e.getValue().equals(secretMap.get(e.getKey()))).count();
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            EvaluationProgram prog = new EvaluationProgram(args[0]);
            prog.runAllEvaluations();
        } else {
            System.err.println("put the pass phrase as param.");
        }
    }
}
