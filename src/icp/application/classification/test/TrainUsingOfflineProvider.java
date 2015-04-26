package icp.application.classification.test;

import icp.Const;
import icp.application.classification.FilterAndSubsamplingFeatureExtraction;
import icp.application.classification.IERPClassifier;
import icp.application.classification.IFeatureExtraction;
import icp.application.classification.MLPClassifier;
import icp.online.app.EpochMessenger;
import icp.online.app.OffLineDataProvider;
import icp.online.app.DataObjects.MessageType;
import icp.online.app.DataObjects.ObserverMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

public class TrainUsingOfflineProvider implements Observer {

	private final List<double[][]> epochs;
	private final List<Double> targets;
	private int numberOfTargets;
	private int numberOfNonTargets;
	private int iters;
	private int middleNeurons;
	private static IFeatureExtraction fe;
	private static IERPClassifier classifier;

	public TrainUsingOfflineProvider(IFeatureExtraction fe, IERPClassifier classifier) {
		this.fe = fe;
		this.classifier = classifier;

		epochs = new ArrayList<double[][]>();
		targets = new ArrayList<Double>();
		numberOfTargets = 0;
		numberOfNonTargets = 0;
		this.iters = 2000;
		this.middleNeurons = 0;

		OffLineDataProvider offLineData = new OffLineDataProvider(new File(
				Const.TRAINING_RAW_DATA_FILE_NAME), this);
		Thread t = new Thread(offLineData);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public TrainUsingOfflineProvider(int iters, int middleNeurons) {

		epochs = new ArrayList<double[][]>();
		targets = new ArrayList<Double>();
		numberOfTargets = 0;
		numberOfNonTargets = 0;
		this.iters = iters;
		this.middleNeurons = middleNeurons;

		OffLineDataProvider offLineData = new OffLineDataProvider(new File(
				Const.TRAINING_RAW_DATA_FILE_NAME), this);
		Thread t = new Thread(offLineData);
		t.start();
		try {
			t.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (classifier == null) {
			TrainUsingOfflineProvider train = new TrainUsingOfflineProvider(
					2000, 8);
		} else {
			TrainUsingOfflineProvider train = new TrainUsingOfflineProvider(fe, classifier);
		}
	}

	@Override
	public void update(Observable sender, Object message) {
		if (message instanceof ObserverMessage) {
			ObserverMessage msg = (ObserverMessage) message;
			if (msg.getMsgType() == MessageType.END) {
				this.train();
			}
		}
		if (message instanceof EpochMessenger) {
			double[][] epoch = ((EpochMessenger) message).getEpoch();
			int stimulus = ((EpochMessenger) message).getStimulusIndex();

			// 1 = target, 3 = non-target
			if (stimulus == 1 && numberOfTargets <= numberOfNonTargets) {
				epochs.add(epoch);
				targets.add(1.0);
				numberOfTargets++;
			} else if (stimulus == 3 && numberOfTargets >= numberOfNonTargets) {
				epochs.add(epoch);
				targets.add(0.0);
				numberOfNonTargets++;
			}
		}
	}

	private void train() {
		// create classifiers
		if (classifier == null) {
			fe = new FilterAndSubsamplingFeatureExtraction();
			int numberOfInputNeurons = fe.getFeatureDimension();
			int middleNeurons = this.middleNeurons;
			int outputNeurons = 1;
			ArrayList<Integer> nnStructure = new ArrayList<Integer>();
			nnStructure.add(numberOfInputNeurons);
			nnStructure.add(middleNeurons);
			nnStructure.add(outputNeurons);
			// classifier = new KNNClassifier();
			classifier = new MLPClassifier(nnStructure);
			// classifier = new JavaMLClassifier();
			classifier.setFeatureExtraction(fe);
		}

		// training
		System.out.println("Training started.");
		classifier.train(this.epochs, this.targets, this.iters, fe);
		classifier.save(Const.TRAINING_FILE_NAME);
		System.out.println("Training finished.");
	}

	public IERPClassifier getClassifier() {
		return this.classifier;
	}

}
