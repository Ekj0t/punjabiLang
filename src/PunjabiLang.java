import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

public class PunjabiLang {
    static Map<String, Object> variables = new HashMap<>();
    static Scanner scanner = new Scanner(System.in, StandardCharsets.UTF_8);

    public static void main(String[] args) throws IOException {
//        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("sample.punjabi"), "UTF-8"));
//        String line;
//
//        while ((line = reader.readLine()) != null) {
//            executeLine(line.trim());
//        }
//
//        reader.close();
        List<String> lines = Files.readAllLines(Paths.get("sample.punjabi"), StandardCharsets.UTF_8);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty() || line.startsWith("//")) continue;
            executeLine(line);  // ← YOU MISSED THIS
        }

    }

    private static void executeLine(String line) {
        if (line.isEmpty()) return;

        // 🔹 Skip full-line comments
        if (line.trim().startsWith("//")) return;

        // 🔹 Remove inline comments (everything after //)
        int commentIndex = line.indexOf("//");
        if (commentIndex != -1) {
            line = line.substring(0, commentIndex).trim();
            if (line.isEmpty()) return;  // Only comment after trimming
        }

        // 🔹 Process statement
        if (line.startsWith("ਅੰਕ")) {
            handleIntDeclaration(line);
        } else if (line.startsWith("ਵਾਕ")) {
            handleStringDeclaration(line);
        } else if (line.startsWith("ਲਿਖੋ")) {
            handlePrint(line);
        } else if (line.startsWith("ਆਣਦੇ")) {
            handleInput(line);
        } else {
            System.err.println("ਯਾਰ ਹਾ ਕੀ ਲਿੱਖਦਾ ਪਿਆ ਆ : " + line);
        }
    }

    private static void handleStringDeclaration(String line) {
        line = line.replace("ਵਾਕ", "").replace(";", "").trim();

        if (line.contains("=")) {
            String[] parts = line.split("=", 2);
            if (parts.length != 2) {
                System.err.println("ਗਲਤ ਵਾਕ ਘੋਸ਼ਣਾ: " + line);
                return;
            }

            String name = parts[0].trim();
            String value = parts[1].trim();

            if (value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1); // Remove quotes
                variables.put(name, value);
            } else {
                System.err.println("ਅਵੈਧ ਵਾਕ ਮੁੱਲ: " + value);
            }
        } else {
            // Uninitialized string defaults to empty
            variables.put(line.trim(), "");
        }
    }

    private static void handleIntDeclaration(String line) {
        line = line.replace("ਅੰਕ", "").replace(";", "").trim();

        if (!line.contains("=")) {
            variables.put(line.trim(), 0);  // default
            return;
        }

        String[] parts = line.split("=");
        if (parts.length != 2) {
            System.err.println("ਗਲਤ ਸੰਰਚਨਾ: " + line);
            return;
        }

        String name = parts[0].trim();
        String expr = parts[1].trim();

        try {
            int result = evaluateExpression(expr);
            variables.put(name, result);
        } catch (Exception e) {
            System.err.println("ਅਵੈਧ ਅੰਕ ਗਣਨਾ: " + expr);
        }
    }

    private static int evaluateExpression(String expr) {
        // Support + - * / with optional spaces
        String[] tokens = expr.split(" ");

        if (tokens.length == 1) {
            return getValue(tokens[0]);
        }

        int result = getValue(tokens[0]);

        for (int i = 1; i < tokens.length - 1; i += 2) {
            String op = tokens[i];
            int nextVal = getValue(tokens[i + 1]);

            switch (op) {
                case "+": result += nextVal; break;
                case "-": result -= nextVal; break;
                case "*": result *= nextVal; break;
                case "/": result /= nextVal; break;
                default:
                    throw new IllegalArgumentException("ਅਵੈਧ ਸੰਚਾਲਕ: " + op);
            }
        }

        return result;
    }

    private static int getValue(String token) {
        token = token.trim();
        if (variables.containsKey(token)) {
            Object val = variables.get(token);
            if (val instanceof Integer) return (int) val;
            else throw new IllegalArgumentException("ਇਹ ਅੰਕ ਨਹੀਂ: " + token);
        } else {
            return Integer.parseInt(token);
        }
    }

    private static void handleInput(String line) {
        line = line.replace("ਆਣਦੇ", "").replace(";", "").trim();

        if (!variables.containsKey(line)) {
            System.err.println("🚫 ਪਹਿਲਾਂ ਘੋਸ਼ਿਤ ਕਰੋ: " + line);
            return;
        }

        System.out.print(": ");
        Object current = variables.get(line);

        if (current instanceof Integer) {
            try {
                int input = Integer.parseInt(scanner.nextLine().trim()); // added trim here
                variables.put(line, input);
            } catch (NumberFormatException e) {
                System.err.println("🚫 ਅਵੈਧ ਅੰਕ: ਕਿਰਪਾ ਕਰਕੇ ਸਹੀ ਨੰਬਰ ਦਿਓ।");
            }
        } else if (current instanceof String) {
            String input = scanner.nextLine().trim();
            variables.put(line, input);
        } else {
            System.err.println("🚫 ਅਣਜਾਣ ਡਾਟਾ ਕਿਸਮ: " + line);
        }
    }

    private static void handlePrint(String line) {
        line = line.replace("ਲਿਖੋ", "").replace(";", "").trim();

        if (line.isEmpty()) return;

        StringBuilder output = new StringBuilder();
        StringBuilder token = new StringBuilder();
        boolean insideQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                insideQuotes = !insideQuotes;
                continue; // skip the quote
            }

            if (c == '+' && !insideQuotes) {
                processToken(token.toString(), output, false); // don't trim here
                token.setLength(0);
            } else {
                token.append(c);
            }
        }

        if (token.length() > 0) {
            processToken(token.toString(), output, false);
        }

        if (insideQuotes) {
            System.err.println("🚫 ਗਲਤ string ਲਿਟਰਲ: quotes not closed properly.");
            return;
        }

        System.out.println(output);
    }

    private static void processToken(String token, StringBuilder output, boolean isInsideQuotes) {
        if (token.isEmpty()) return;

        // If it’s a variable
        if (variables.containsKey(token.trim())) {
            Object value = variables.get(token.trim());
            output.append(String.valueOf(value));
        } else {
            // Literal string (already unquoted, keep spaces)
            output.append(token);
        }
    }

    private static String getStringValue(String token) {
        if (token.startsWith("\"") && token.endsWith("\"")) {
            return token.substring(1, token.length() - 1); // string literal
        } else if (variables.containsKey(token)) {
            Object val = variables.get(token);
            if (val instanceof String) return (String) val;
        }
        return ""; // fallback
    }

    private static int getIntValue(String token) {
        if (variables.containsKey(token)) {
            Object val = variables.get(token);
            if (val instanceof Integer) return (Integer) val;
        }
        return Integer.parseInt(token); // fallback for literal
    }

}
