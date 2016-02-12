package com.uom.cse.distsearch.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;


public class MovieList {
    /**
     * Logger to log the events.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(MovieList.class);

    private static MovieList instance;

    private List<String> movies = new ArrayList<String>();

    public static MovieList getInstance(String path) {
        if (instance == null) {
            synchronized (MovieList.class) {
                if (instance == null) {
                    instance = new MovieList(path);
                }
            }
        }

        return instance;
    }

    private MovieList(String fileName) {
        this.movies = selectMovies(fileName);
    }

    public List<String> search(String query) {
        List<String> list = new ArrayList<String>();

        if (query != null && !query.trim().equals("")) {
            query = query.toLowerCase();
            for (String movie : movies) {
                if (movie.toLowerCase().contains(query)) {
                    // Remove the spaces
                    list.add(movie.replaceAll(" ", "_"));
                }
            }
        }
        return list;
    }

    private List<String> selectMovies(String fileName) {
        List<String> list = new ArrayList<>();
        List<String> movies = new ArrayList<String>();

        try (Scanner scanner = new Scanner(new File(fileName))) {
            while (scanner.hasNextLine()) {
                list.add(scanner.nextLine());
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e.getMessage(), e);
        }

        Collections.shuffle(list);

        Random rand = new Random();
        int num = rand.nextInt(3) + 3;
        for (int i = 0; i < num; i++) {
            movies.add(list.get(i));
        }

        return movies;
    }

    public List<String> getSelectedMovies() {
        return this.movies;
    }

    @Override
    public String toString() {
        return movies.toString();
    }
}
