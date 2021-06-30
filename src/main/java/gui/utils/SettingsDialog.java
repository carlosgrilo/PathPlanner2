package gui.utils;

import gui.PathPlanner;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SettingsDialog extends JDialog implements ItemListener {

    public final int MAX_NUM_AGENTS = 5;
    public final int MIN_AVERAGE_PICKS_PER_TASK = 1;
    public final int MAX_AVERAGE_PICKS_PER_TASK = 20;

    public int erpCheckPeriod; //MINUTOS
    public float corridorWidth;
    public int minNumberAgents = 1;
    public int minAveragePicksPerTask = 2;
    public int maxAveragePicksPerTask = 10;
    public String clientID;

    public PathPlanner frame;
    public JTextField nomeInput;
    public JSpinner width;
    public JSpinner checkERPPeriod;
    public JSpinner minNumberAgentsJSpinner;
    public JSpinner minAveragePicksPerTaskJSpinner;
    public JSpinner maxAveragePicksPerTaskJSpinner;
    public JToggleButton toggleButton;
    public boolean cancel;

    public SettingsDialog(
            PathPlanner frame,
            int erpCheckPeriod,
            float corridorWidth,
            String clientID,
            int minNumberAgents,
            int minAveragePicksPerTask,
            int maxAveragePicksPerTask) {

        cancel = false;

        this.frame = frame;
        this.erpCheckPeriod = erpCheckPeriod;
        this.corridorWidth = corridorWidth;
        this.clientID = clientID;
        this.minNumberAgents = minNumberAgents;
        this.minAveragePicksPerTask = minAveragePicksPerTask;
        this.maxAveragePicksPerTask = maxAveragePicksPerTask;
        this.setModal(true);
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        JLabel name_label = new JLabel("ESB identifier for PathPlanner:");
        add(name_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        JLabel period_label = new JLabel("Task polling period (min):");
        add(period_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        JLabel corridor_label = new JLabel("Minimum width for operator (m):");
        add(corridor_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        JLabel minNumAgent_label = new JLabel("Minimum number of operators:");
        add(minNumAgent_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        JLabel minAveragePicksPerCluster_label = new JLabel("Min. average picks/cluster:");
        add(minAveragePicksPerCluster_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        JLabel maxAveragePicksPerCluster_label = new JLabel("Max. average picks/cluster:");
        add(maxAveragePicksPerCluster_label, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        JLabel checkERPTasks_label = new JLabel("Auto check ERP tasks");
        add(checkERPTasks_label, gbc);

        gbc.gridx = 1;
        gbc.gridy = 0;
        nomeInput = new JTextField(clientID);
        add(nomeInput, gbc);

        gbc.gridx = 1;
        gbc.gridy = 1;
        SpinnerModel model= new SpinnerNumberModel(erpCheckPeriod,1,30,1);
        checkERPPeriod = new JSpinner(model);
        add(checkERPPeriod, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
        SpinnerModel model2 = new SpinnerNumberModel(corridorWidth,0.5,3,0.1);
        width = new JSpinner(model2);
        add(width, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        SpinnerModel model3 = new SpinnerNumberModel(frame.getRequestsState().getMinNumAgents(), 1, MAX_NUM_AGENTS,1);
        minNumberAgentsJSpinner = new JSpinner(model3);
        add(minNumberAgentsJSpinner, gbc);

        gbc.gridx = 1;
        gbc.gridy = 4;
        SpinnerModel model4 = new SpinnerNumberModel(frame.getRequestsState().getMinAveragePicksPerTask(), MIN_AVERAGE_PICKS_PER_TASK, MAX_AVERAGE_PICKS_PER_TASK,1);
        minAveragePicksPerTaskJSpinner = new JSpinner(model4);
        add(minAveragePicksPerTaskJSpinner, gbc);

        gbc.gridx = 1;
        gbc.gridy = 5;
        SpinnerModel model5 = new SpinnerNumberModel(frame.getRequestsState().getMaxAveragePicksPerTask(), MIN_AVERAGE_PICKS_PER_TASK, MAX_AVERAGE_PICKS_PER_TASK,1);
        maxAveragePicksPerTaskJSpinner = new JSpinner(model5);
        add(maxAveragePicksPerTaskJSpinner, gbc);

        gbc.gridx = 1;
        gbc.gridy = 6;
        toggleButton = new JToggleButton(frame.isCheckERPTasksAutomatically()? "On" : "Off");
        toggleButton.setSelected(frame.isCheckERPTasksAutomatically());
        toggleButton.addItemListener(this);
        add(toggleButton, gbc);

        gbc.gridx = 0;
        gbc.gridy = 7;
        JButton ok_button = new JButton("OK");
        add(ok_button, gbc);
        ok_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateValues();
                dispose();
            }
        });

        gbc.gridx = 1;
        gbc.gridy = 7;
        JButton cancel_button = new JButton("Cancel");
        add(cancel_button, gbc);
        cancel_button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancel = true;
                dispose();
            }
        });
        pack();
        this.setVisible(true);
    }

    public void updateValues(){
        clientID = nomeInput.getText();
        corridorWidth = ((Number) width.getValue()).floatValue();
        erpCheckPeriod = ((Number) checkERPPeriod.getValue()).intValue();
        minNumberAgents = ((Number) minNumberAgentsJSpinner.getValue()).intValue();
        minAveragePicksPerTask = ((Number) minAveragePicksPerTaskJSpinner.getValue()).intValue();
        maxAveragePicksPerTask = ((Number) maxAveragePicksPerTaskJSpinner.getValue()).intValue();
        if(minAveragePicksPerTask > maxAveragePicksPerTask){
            minAveragePicksPerTask = maxAveragePicksPerTask;
            JOptionPane.showMessageDialog(frame, "WARNING: AVERAGE MIN VALUE OF PICKS PER TASK LARGER THAN MAX VALUE!!!\n MIN VALUE WAS ADJUSTED TO MAX");
        }
    }

    public void itemStateChanged(ItemEvent eve) {
        toggleButton.setText(toggleButton.isSelected()? "On" : "Off");
    }
}
