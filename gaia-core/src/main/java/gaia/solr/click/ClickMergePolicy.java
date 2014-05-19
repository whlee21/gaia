package gaia.solr.click;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.MergePolicy;
import org.apache.lucene.index.SegmentInfo;
import org.apache.lucene.index.SegmentCommitInfo;
import org.apache.lucene.index.SegmentInfos;

public class ClickMergePolicy extends MergePolicy {
	boolean doMerge = true;
	int[] targets;
	private final boolean useCompoundFile;

	ClickMergePolicy(int[] targets, boolean useCompoundFile) {
		this.useCompoundFile = useCompoundFile;
		this.targets = targets;
	}

	public void close() {
	}

	public MergePolicy.MergeSpecification findMerges(MergePolicy.MergeTrigger mergeTrigger, SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		MergePolicy.MergeSpecification ms = new MergePolicy.MergeSpecification();
		if (doMerge) {
			List<SegmentCommitInfo> mergeInfos = new ArrayList<SegmentCommitInfo>();
			int target = 0;
			int count = 0;
			for (int i = 0; (i < segmentInfos.size()) && (target < targets.length); i++) {
				SegmentCommitInfo commit = segmentInfos.info(i);
				SegmentInfo info = commit.info;
				if (info.getDocCount() == targets[target]) {
					target++;
				} else if (count + info.getDocCount() <= targets[target]) {
					mergeInfos.add(commit);
					count += info.getDocCount();
				} else {
					assert (info.getDocCount() < targets[target]) : "doc count should be smaller than the current target";
					if (mergeInfos.size() > 0) {
						MergePolicy.OneMerge om = new MergePolicy.OneMerge(mergeInfos);
						ms.add(om);
					}
					count = 0;
					mergeInfos = new ArrayList<SegmentCommitInfo>();
				}
			}
			if (mergeInfos.size() > 0) {
				MergePolicy.OneMerge om = new MergePolicy.OneMerge(mergeInfos);
				ms.add(om);
			}

			return ms;
		}
		return null;
	}

	public MergePolicy.MergeSpecification findForcedMerges(SegmentInfos segmentInfos, int maxSegmentCount,
			Map<SegmentCommitInfo, Boolean> segmentsToOptimize) throws CorruptIndexException, IOException {
		System.err.println("findForcedMerges");
		return null;
	}

	public MergePolicy.MergeSpecification findForcedDeletesMerges(SegmentInfos segmentInfos)
			throws CorruptIndexException, IOException {
		System.err.println("findForcedDeletesMerges");
		return null;
	}

	public boolean useCompoundFile(SegmentInfos segments, SegmentCommitInfo newSegment) {
		return useCompoundFile;
	}
}
