package eu.maveniverse.maven.mima.cli;

import eu.maveniverse.maven.mima.context.Context;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jline.builtins.ConfigurationPath;
import org.jline.console.SystemRegistry;
import org.jline.console.impl.Builtins;
import org.jline.console.impl.SystemRegistryImpl;
import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.MaskingCallback;
import org.jline.reader.Parser;
import org.jline.reader.Reference;
import org.jline.reader.UserInterruptException;
import org.jline.reader.impl.DefaultParser;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;
import org.jline.terminal.impl.jansi.JansiTerminalProvider;
import org.jline.widget.TailTipWidgets;
import picocli.CommandLine;
import picocli.shell.jline3.PicocliCommands;

@CommandLine.Command(name = "repl", description = "REPL console")
public class Repl extends CommandSupport {
    @Override
    protected Integer doCall(Context context) {
        Class<?> tp = JansiTerminalProvider.class;

        // set up JLine built-in commands
        ConfigurationPath configPath = new ConfigurationPath(Paths.get("."), Paths.get("."));
        Builtins builtins = new Builtins(Repl::workDir, configPath, null);
        builtins.rename(org.jline.console.impl.Builtins.Command.TTOP, "top");
        builtins.alias("zle", "widget");
        builtins.alias("bindkey", "keymap");
        // set up picocli commands
        CommandLine cmd = new CommandLine(new Main());
        PicocliCommands picocliCommands = new PicocliCommands(cmd);

        Parser parser = new DefaultParser();

        try (Terminal terminal = TerminalBuilder.builder().name("mima").build()) {
            SystemRegistry systemRegistry = new SystemRegistryImpl(parser, terminal, Repl::workDir, configPath);
            systemRegistry.setCommandRegistries(builtins, picocliCommands);

            LineReader reader = LineReaderBuilder.builder()
                    .terminal(terminal)
                    .completer(systemRegistry.completer())
                    .parser(parser)
                    .variable(LineReader.LIST_MAX, 50) // max tab completion candidates
                    .build();
            builtins.setLineReader(reader);
            new TailTipWidgets(reader, systemRegistry::commandDescription, 5, TailTipWidgets.TipType.COMPLETER);
            KeyMap<Binding> keyMap = reader.getKeyMaps().get("main");
            keyMap.bind(new Reference("tailtip-toggle"), KeyMap.alt("s"));

            String prompt = "prompt> ";
            String rightPrompt = null;

            // start the shell and process input until the user quits with Ctrl-D
            String line;
            while (true) {
                try {
                    systemRegistry.cleanUp();
                    line = reader.readLine(prompt, rightPrompt, (MaskingCallback) null, null);
                    systemRegistry.execute(line);
                } catch (UserInterruptException e) {
                    // Ignore
                } catch (EndOfFileException e) {
                    return 0;
                } catch (Exception e) {
                    systemRegistry.trace(e);
                    return 2;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return 1;
        }
    }

    private static Path workDir() {
        return Paths.get(System.getProperty("user.dir"));
    }
}
