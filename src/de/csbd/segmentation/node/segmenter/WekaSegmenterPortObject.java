package de.csbd.segmentation.node.segmenter;

import java.io.IOException;
import java.util.List;
import java.util.zip.ZipEntry;

import javax.swing.JComponent;

import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.port.AbstractPortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortObjectZipInputStream;
import org.knime.core.node.port.PortObjectZipOutputStream;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.PortTypeRegistry;
import org.knime.knip.core.KNIPGateway;

import net.imglib2.algorithm.features.classification.Classifier;

public class WekaSegmenterPortObject extends AbstractPortObject {

    public static final class Serializer extends AbstractPortObjectSerializer<WekaSegmenterPortObject> {
    }

    public static final PortType TYPE = PortTypeRegistry.getInstance().getPortType(WekaSegmenterPortObject.class);

    private static final String SUMMARY = "Trainable Segmentation Model";

    private Classifier model;

    private WekaSegmenterPortObjectSpec spec;

    /** Framework constructor. */
    public WekaSegmenterPortObject() {
    }

    /**
     * Constructor for class DLModelPortObject.
     *
     * @param layers the list of layers contained in this model
     * @param model the {@link Model} which should be stored
     * @param spec the specs corresponding to this object
     */
    public WekaSegmenterPortObject(final Classifier model, final WekaSegmenterPortObjectSpec spec) {
        this.spec = spec;
        this.model = model;
    }

    @Override
    protected void save(final PortObjectZipOutputStream out, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    	out.putNextEntry(new ZipEntry("Classifier"));
        model.store(out);
    }

    @Override
    protected void load(final PortObjectZipInputStream in, final PortObjectSpec spec, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {

        this.spec = (WekaSegmenterPortObjectSpec)spec;
        ZipEntry zipEntry = in.getNextEntry();
        this.model = Classifier.load(KNIPGateway.ops(), in);
    }

    @Override
    public String getSummary() {
        return SUMMARY;
    }

    @Override
    public PortObjectSpec getSpec() {
        return spec;
    }

    public Classifier getModel() {
        return model;
    }

    @Override
    public JComponent[] getViews() {
        return new JComponent[]{};
    }
}
