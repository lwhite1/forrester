package com.deathrayresearch.forrester.event;

import com.deathrayresearch.forrester.model.Model;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.opencsv.CSVWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileWriter;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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

        List<String> values = new ArrayList<>();
        values.add(event.getCurrentTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        for (int i = 0; i < model.getStockValues().size(); i++) {
            values.add(String.valueOf(model.getStockValues().get(i)));
        }
        for (int i = 0; i < model.getVariableValues().size(); i++) {
            values.add(String.valueOf(model.getVariableValues().get(i)));
        }
        csvWriter.writeNext(values.toArray(new String[values.size()]));
    }

    @Override
    @Subscribe
    public void handleSimulationStartEvent(SimulationStartEvent event) {
        logger.info("Starting simulation: " + event.getModel().getName());

        Model model = event.getModel();
        List<String> values = new ArrayList<>();
        values.add("Date time");
        for (int i = 0; i < model.getStockNames().size(); i++) {
            values.add(model.getStockNames().get(i));
        }
        for (int i = 0; i < model.getVariableNames().size(); i++) {
            values.add(model.getVariableNames().get(i));
        }
        csvWriter.writeNext(values.toArray(new String[model.getStockNames().size() + 1]));
    }

    @Override
    @Subscribe
    public void handleSimulationEndEvent(SimulationEndEvent event) {
        try {
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("Ending simulation");
    }
}