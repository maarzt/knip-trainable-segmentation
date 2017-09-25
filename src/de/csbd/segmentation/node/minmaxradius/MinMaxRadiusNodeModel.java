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
package de.csbd.segmentation.node.minmaxradius;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.MissingCell;
import org.knime.core.data.RowKey;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
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
import org.knime.core.util.Pair;
import org.knime.knip.base.data.labeling.LabelingCell;
import org.knime.knip.base.data.labeling.LabelingValue;
import org.knime.knip.base.node.NodeUtils;
import org.knime.knip.base.node.nodesettings.SettingsModelDimSelection;
import org.knime.knip.core.KNIPGateway;
import org.scijava.log.LogService;

import net.imagej.axis.DefaultAxisType;
import net.imagej.ops.Ops.Geometric.Centroid;
import net.imagej.ops.Ops.Geometric.Contour;
import net.imagej.ops.slice.SlicesII;
import net.imagej.ops.special.function.Functions;
import net.imagej.ops.special.function.UnaryFunctionOp;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealLocalizable;
import net.imglib2.roi.geometric.Polygon;
import net.imglib2.roi.labeling.LabelRegion;
import net.imglib2.roi.labeling.LabelRegions;
import net.imglib2.roi.labeling.LabelingType;
import net.imglib2.type.numeric.RealType;

/**
 * MinMaxRadiusNodeModel.
 * 
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
public class MinMaxRadiusNodeModel<L extends Comparable<L>, O extends RealType<O>> extends NodeModel {

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
	 * Settings model of the dimension selection.
	 */
	private SettingsModelDimSelection dimSelection = createDimSelection();

	/**
	 * Create a settings model for the dimension selection component.
	 * 
	 * @return SettingsModelDimSelection
	 */
	protected static SettingsModelDimSelection createDimSelection() {
		return new SettingsModelDimSelection("DimSelection", new DefaultAxisType("X"), new DefaultAxisType("Y"));
	}

	/**
	 * KNIP logger instance.
	 */
	private static final LogService LOGGER = KNIPGateway.log();

	/**
	 * The centroid function from imagej-ops.
	 */
	private UnaryFunctionOp<LabelRegion<L>, RealLocalizable> centroidFunction;

	/**
	 * The LabelRegion to Polygon converter from imagej-ops.
	 */
	private UnaryFunctionOp<LabelRegion<L>, Polygon> converter;

	/**
	 * Constructor of the MinMaxRadiusNodeModel.
	 */
	protected MinMaxRadiusNodeModel() {
		super(1, 1);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
		final DataTableSpec spec = inSpecs[0];

		// Check table spec if column is available.
		NodeUtils.autoColumnSelection(spec, columnSelection, LabelingValue.class, this.getClass());

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
			final DataCell cell = row.getCell(data.getSpec().findColumnIndex(columnSelection.getStringValue()));

			if (cell.isMissing()) {
				// If the cell is missing, insert missing cells and inform user
				// via log.
				container.addRowToTable(new DefaultRow(row.getKey(), new MissingCell(null), new MissingCell(null)));
				LOGGER.warn("Missing cell in row " + row.getKey().getString() + ". Missing cell inserted.");
			} else {
				// Else compute the results.
				addRows(container, (LabelingCell<L>) cell, row.getKey());
			}

			// Update progress indicator.
			exec.setProgress(currentRow++ / numRows);
		}

		container.close();

		return new BufferedDataTable[] { container.getTable() };
	}

	/**
	 * Initiate ops. This is not possible during
	 * {@link MinMaxRadiusNodeModel#configure(DataTableSpec[])} since the data
	 * is not available at the time. The ops are initiated with the first actual
	 * data-instance.
	 * 
	 * @param region
	 *            the first real LabelRegion
	 */
	private void init(final LabelRegion<L> region) {
		centroidFunction = Functions.unary(KNIPGateway.ops(), Centroid.class, RealLocalizable.class, region);
		converter = Functions.unary(KNIPGateway.ops(), Contour.class, Polygon.class, region, true);
	}

	/**
	 * Add for each LabelRegion in a Labeling a new row with the min and max
	 * radius.
	 * 
	 * In this method, the ROIs get sliced to 2D slices which then will be
	 * processed. Since a Label can result in many slices of any size, the
	 * slices are processed concurrently.
	 * 
	 * @param container
	 *            to store the result
	 * @param cell
	 *            the current cell containing the Labeling
	 * @param key
	 *            the current row key
	 * @throws InvalidSettingsException
	 *             if it is not possible to create 2D slices
	 */
	private void addRows(final BufferedDataContainer container, final LabelingCell<L> cell, final RowKey key)
			throws InvalidSettingsException {
		final List<Future<Pair<String, DoubleCell[]>>> futures = new ArrayList<>();

		final int[] selectedDimIndices = dimSelection.getSelectedDimIndices(cell.getLabelingMetadata());
		if (selectedDimIndices.length != 2) {
			// If a selected dimension does not exist, inform the user.
			throw new InvalidSettingsException("Selected dimensions result in none two dimensional ROIs.");
		}

		final SlicesII<LabelingType<L>> slices = new SlicesII<>(cell.getLabeling(), selectedDimIndices, true);

		long sliceCount = 0;
		for (final RandomAccessibleInterval<LabelingType<L>> slice : slices) {
			// Get all ROIs of this slice.
			final LabelRegions<L> regions = KNIPGateway.regions().regions(slice);

			for (final LabelRegion<L> region : regions) {
				final long currentSlice = sliceCount;

				if (centroidFunction == null || converter == null) {
					// Initialize ops with the first available ROI.
					init(region);
				}

				// Process ROIs in parallel
				futures.add(KNIPGateway.threads().run(new Callable<Pair<String, DoubleCell[]>>() {

					@Override
					public Pair<String, DoubleCell[]> call() throws Exception {
						// Make sure that a unique identifier is created.
						return new Pair<>("Region" + region.getLabel().toString() + "_Slice" + currentSlice,
								computeMinMaxRadius(region));
					}
				}));
			}
			sliceCount++;
		}

		for (final Future<Pair<String, DoubleCell[]>> future : futures) {
			try {
				// Collect the results and generate a unique row id for each new
				// row.
				container.addRowToTable(
						new DefaultRow(key.getString() + "_" + future.get().getFirst(), future.get().getSecond()));
			} catch (InterruptedException | ExecutionException e) {
				LOGGER.error(e);
			}
		}

	}

	/**
	 * Compute the min and max radius for one ROI.
	 * 
	 * @param region
	 *            the ROI
	 * @return the min and max radius
	 */
	private DoubleCell[] computeMinMaxRadius(final LabelRegion<L> region) {

		final RealLocalizable centroid = centroidFunction.calculate(region);
		final Polygon poly = converter.calculate(region);

		double minDist = Double.MAX_VALUE;
		double maxDist = 0;
		double tmpDist;
		for (final RealLocalizable v : poly.getVertices()) {
			tmpDist = Math.sqrt(Math.pow(centroid.getDoublePosition(0) - v.getDoublePosition(0), 2)
					+ Math.pow(centroid.getDoublePosition(1) - v.getDoublePosition(1), 2));
			minDist = tmpDist < minDist ? tmpDist : minDist;
			maxDist = tmpDist > maxDist ? tmpDist : maxDist;
		}

		return new DoubleCell[] { new DoubleCell(minDist), new DoubleCell(maxDist) };
	}

	/**
	 * Create the table spec of the output table. In this case a new table with
	 * just two columns is generated.
	 * 
	 * @return table spec with columns "Min Radius" and "Max Radius"
	 */
	private DataTableSpec createDataTableSpec() {
		return new DataTableSpec(new String[] { "Min Radius", "Max Radius" },
				new DataType[] { DoubleCell.TYPE, DoubleCell.TYPE });
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void saveSettingsTo(NodeSettingsWO settings) {
		columnSelection.saveSettingsTo(settings);
		dimSelection.saveSettingsTo(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void validateSettings(NodeSettingsRO settings) throws InvalidSettingsException {
		columnSelection.validateSettings(settings);
		dimSelection.validateSettings(settings);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void loadValidatedSettingsFrom(NodeSettingsRO settings) throws InvalidSettingsException {
		columnSelection.loadSettingsFrom(settings);
		dimSelection.loadSettingsFrom(settings);
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
