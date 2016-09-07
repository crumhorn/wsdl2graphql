/**
 * The MIT License (MIT)
 Copyright (c) 2016 Emil Crumhorn

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
import org.apache.commons.cli.*;

import java.io.File;

public class Main {

    public static void main(String [] args) {

        Options options = new Options();

        Option input = new Option("i", "input", true, "wsdl input file or url");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output file");
        output.setRequired(true);
        options.addOption(output);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.out.println(".wsdl 2 graphql schema parser - by Emil Crumhorn. Last rev: Aug 12, 2016");
            System.err.println(e);
            formatter.printHelp("wsdl2graphql", options);
            System.exit(-1);
            return;
        }

        String wsdlPath = cmd.getOptionValue("input");
        String outputPath = cmd.getOptionValue("output");

        if (wsdlPath.startsWith("http")) {
            new Parser(wsdlPath, outputPath);
        }
        else {
            File f = new File(wsdlPath);
            if (!f.exists()) {
                System.out.println("File '" + f.getAbsolutePath() + "', does not exist");
                System.exit(-1);
                return;
            }

            if (!f.canRead()) {
                System.out.println("File '" + f.getAbsolutePath() + "', cannot be read");
                System.exit(-1);
                return;
            }

            new Parser(f, outputPath);
        }
    }




}