package com.deathrayresearch.dynamics.event;

import com.deathrayresearch.dynamics.model.Model;
import com.deathrayresearch.dynamics.model.Stock;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
public class CsvSubscriber implements EventHandler {

    private static final Logger logger = LoggerFactory.getLogger(CsvSubscriber.class);

    private CSVWriter csvWriter;

    public static CsvSubscriber newInstance(EventBus eventBus, String fileName) {
        CsvSubscriber subscriber = new CsvSubscriber(fileName);
        eventBus.register(subscriber);
        return subscriber;
    }

    private CsvSubscriber(String fileName) {
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        csvWriter = new CSVWriter(fileWriter);
    }

    @Subscribe
    public void handleTimestepEvent(TimestepEvent event) {
        Model model = event.getModel();
        String[] values = new String[model.getStocks().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = String.valueOf(model.getStockValues().get(i));
        }
        csvWriter.writeNext(values);
    }

    @Override
    @Subscribe
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        logger.info("Starting simulation: " + event.getModel().getName());

        Model model = event.getModel();
        String[] values = new String[model.getStocks().size()];
        for (int i = 0; i < values.length; i++) {
            values[i] = model.getStockNames().get(i);
        }
        csvWriter.writeNext(values);
    }

    @Override
    @Subscribe
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        logger.info("Ending simulation");
    }
}
