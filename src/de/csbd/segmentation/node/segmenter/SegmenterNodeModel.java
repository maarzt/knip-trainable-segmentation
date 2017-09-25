/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2017
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package de.csbd.segmentation.node.segmenter;

import java.io.File;
import java.io.IOException;

import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.knip.base.data.img.ImgPlusCell;
import org.knime.knip.base.data.img.ImgPlusCellFactory;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.core.KNIPGateway;
import org.scijava.log.LogService;

import net.imagej.ImgPlus;
import net.imglib2.Cursor;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.type.numeric.RealType;

/**
 * MinMaxRadiusNodeModel.
 * 
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
public class SegmenterNodeModel<T extends RealType<T>, O extends RealType<O>> extends NodeModel {

	/**
	 * Settings model of the column selection.
	 */
	private SettingsModelString columnSelection = createColumnSelection();

	/**
	 * Create a settings model for the column selection component.
	 * 
	 * @return SettingsModelString
	 */
	protected static SettingsModelString createColumnSelection() {
		return new SettingsModelString("ColumnSelection", "");
	}

	/**
	 * KNIP logger instance.
	 */
	private static final LogService LOGGER = KNIPGateway.log();

	/**
	 * Constructor of the MinMaxRadiusNodeModel.
	 */
	protected SegmenterNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		final DataTableSpec spec = inSpecs[0];

		// Check table spec if column is available.
		NodeUtils.autoColumnSelection(spec, columnSelection, ImgPlusValue.class, this.getClass());

		// If everything looks fine, create an output table spec.
		return new DataTableSpec[] { createDataTableSpec() };
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
			throws Exception {
		// The datatable passed by the first dataport.
		final BufferedDataTable data = inData[0];

		// Variables to compute progress.
		final double numRows = data.size();
		long currentRow = 0;

		// Create a container to store the output.
		final BufferedDataContainer container = exec.createDataContainer(createDataTableSpec());

		for (final DataRow row : data) {
			// Check if execution got canceled.
			exec.checkCanceled();

			// Get the data cell.
			final ImgPlusCell<T> cell = (ImgPlusCell<T>)row.getCell(data.getSpec().findColumnIndex(columnSelection.getStringValue()));
			final ImgPlusCellFactory imgPlusCellFactory = new ImgPlusCellFactory(exec);

			if (cell.isMissing()) {
				// If the cell is missing, insert missing cells and inform user
				// via log.
				container.addRowToTable(new DefaultRow(row.getKey(), new MissingCell(null), new MissingCell(null)));
				LOGGER.warn("Missing cell in row " + row.getKey().getString() + ". Missing cell inserted.");
			} else {
				// Else copy
				
				// Simple copy
				// container.addRowToTable(new DefaultRow(row.getKey(), imgPlusCellFactory.createCell(cell.getImgPlusCopy())));
				
				// Copy with iterator and randomAccess
				final ImgPlus<T> original = cell.getImgPlus();
				final Img<T> copy = original.factory().create(original, original.firstElement());
				
				final Cursor<T> originalC = original.localizingCursor();
				final RandomAccess<T> copyRA = copy.randomAccess();
				
				while (originalC.hasNext()) {
					originalC.fwd();
					copyRA.setPosition(originalC);
					
					copyRA.get().set(originalC.get());
				}
				
				container.addRowToTable(new DefaultRow(row.getKey(), imgPlusCellFactory.createCell(new ImgPlus<>(copy))));
			}

			// Update progress indicator.
			exec.setProgress(currentRow++ / numRows);
		}

		container.close();

		return new BufferedDataTable[] { container.getTable() };
	}

	/**
	 * Create the table spec of the output table. I
	 * 
	 * @return table spec with column "Copy"
	 */
	private DataTableSpec createDataTableSpec() {
		return new DataTableSpec(new String[] { "Copy"},
				new DataType[] { ImgPlusCell.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		columnSelection.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		columnSelection.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		columnSelection.loadSettingsFrom(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Ex.: Models built during execution.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveInternals(File nodeInternDir, ExecutionMonitor exec)
			throws IOException, CanceledExecutionException {
		// Ex.: Models built during execution.
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void reset() {
		// Ex.: Models built during execution.
	}

}
