package com.tinkerpop.bench.benchmark;

import java.io.File;
import java.util.ArrayList;

import com.tinkerpop.bench.BenchRunner;
import com.tinkerpop.bench.GraphDescriptor;
import com.tinkerpop.bench.operationFactory.OperationFactoryLog;
import com.tinkerpop.bench.operationFactory.OperationFactory;

/**
 * @author Alex Averbuch (alex.averbuch@gmail.com)
 */

public abstract class Benchmark {

	private String opLog = null;

	public Benchmark(String opLog) {
		this.opLog = opLog;
	}

	private void createOperationLogs(GraphDescriptor graphDescriptor) throws Exception {
		try {
			BenchRunner benchRunner = new BenchRunner(graphDescriptor,
					new File(opLog), getOperationFactories());

			benchRunner.startBench();
		} catch (Exception e) {
			throw e;
		}
	}

	protected abstract ArrayList<OperationFactory> getOperationFactories();
	
	public final void loadOperationLogs(GraphDescriptor graphDescriptor,
			String resultLog) throws Exception {
		if (new File(opLog).exists() == false)
			createOperationLogs(graphDescriptor);

		OperationFactory operationFactory = new OperationFactoryLog(new File(
				opLog));

		BenchRunner benchRunner = new BenchRunner(graphDescriptor, new File(
				resultLog), operationFactory);

		benchRunner.startBench();
	}
	
}