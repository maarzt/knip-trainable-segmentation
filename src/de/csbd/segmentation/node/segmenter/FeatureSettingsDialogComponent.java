package de.csbd.segmentation.node.segmenter;

import java.awt.Dimension;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponent;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.knip.core.KNIPGateway;

import net.imglib2.algorithm.features.FeatureGroup;
import net.imglib2.algorithm.features.Features;
import net.imglib2.algorithm.features.GlobalSettings;
import net.imglib2.algorithm.features.SingleFeatures;
import net.imglib2.algorithm.features.gson.FeaturesGson;
import net.imglib2.algorithm.features.gui.FeatureSettingsGui;
import net.imglib2.algorithm.features.ops.FeatureOp;

/**
 * @author Matthias Arzt
 */
public final class FeatureSettingsDialogComponent extends DialogComponent {

    private final FeatureSettingsGui gui;

    /**
     * Constructor put label and JTextField into panel. It will accept empty
     * strings as legal input.
     *
     * @param stringModel the model that stores the value for this component.
     * @param label label for dialog in front of JTextField
     */
    public FeatureSettingsDialogComponent(final SettingsModelString stringModel) {
        super(stringModel);

        //m_label = new JLabel(label);
        //getComponentPanel().add(m_label);

        FeatureGroup fg = featureGroupFromString(stringModel.getStringValue());
        gui = new FeatureSettingsGui(KNIPGateway.ops().context(), fg);

        getComponentPanel().add(gui.getComponent());

        updateComponent();
    }

    private FeatureGroup featureGroupFromString(String json) {
    	if(json.isEmpty()) {
    		GlobalSettings settings = GlobalSettings.defaultSettings();
    		FeatureOp identity = new SingleFeatures(KNIPGateway.ops(), settings).identity();
    		return Features.group(identity);
    	}
    	return FeaturesGson.fromJson(json, KNIPGateway.ops());
	}

	/**
     * {@inheritDoc}
     */
    @Override
    protected void updateComponent() {
    	// not supported so far
        setEnabledComponents(getModel().isEnabled());
    }

    /**
     * Transfers the current value from the component into the model.
     *
     * @throws InvalidSettingsException if the string was not accepted.
     */
    private void updateModel() throws InvalidSettingsException {
        // we transfer the value from the field into the model
        ((SettingsModelString)getModel())
                .setStringValue(FeaturesGson.toJson(gui.get()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettingsBeforeSave()
            throws InvalidSettingsException {
        updateModel();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void checkConfigurabilityBeforeLoad(final PortObjectSpec[] specs)
            throws NotConfigurableException {
        // we are always good.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setEnabledComponents(final boolean enabled) {
        gui.getComponent().setEnabled(enabled);
    }

    /**
     * Sets the preferred size of the internal component.
     *
     * @param width The width.
     * @param height The height.
     */
    public void setSizeComponents(final int width, final int height) {
        gui.getComponent().setPreferredSize(new Dimension(width, height));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setToolTipText(final String text) {
        gui.getComponent().setToolTipText(text);
    }

}