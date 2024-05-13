package org.example;

import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public class RobotRespector {

    private String robotsTxtPath;

    public RobotRespector(String robotsTxtPath) {
        this.robotsTxtPath = robotsTxtPath;
    }

    private byte[] readRobotsTxt() throws ParseException {
        try {
            if (Objects.isNull(robotsTxtPath)) {
                // Reading from stdin
                return ByteStreams.toByteArray(System.in);
            } else {
                // Reading from file
                return Files.readAllBytes(Path.of(robotsTxtPath));
            }
        } catch (final UncheckedIOException | IOException | InvalidPathException e) {
            throw new ParseException("Failed to read robots.txt file.", e);
        }
    }

    public boolean isAllowed(List<String> agents, String url) {
        final byte[] robotsTxtContents;
        try {
            robotsTxtContents = readRobotsTxt();
        } catch (final ParseException e) {
            System.err.println(e.getMessage());
            return false;
        }

        final Parser parser = new RobotsParser(new RobotsParseHandler());
        final RobotsMatcher matcher = (RobotsMatcher) parser.parse(robotsTxtContents);

        final boolean parseResult;
        parseResult = matcher.allowedByRobots(agents, url);

        if (parseResult) {
            System.out.println(url + " is ALLOWED");
            return true;
        } else {
            System.out.println(url + " is DISALLOWED");
            return false;
        }
    }


}
