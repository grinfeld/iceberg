package com.mikerusoft.examples.compaction

/*
  Name	Default Value	Description
    max-concurrent-file-group-rewrites	5	Maximum number of file groups to be simultaneously rewritten
    partial-progress.enabled	false	Enable committing groups of files prior to the entire rewrite completing
    partial-progress.max-commits	10	Maximum amount of commits that this rewrite is allowed to produce if partial progress is enabled
    partial-progress.max-failed-commits	value of partital-progress.max-commits	Maximum amount of failed commits allowed before job failure, if partial progress is enabled
    use-starting-sequence-number	true	Use the sequence number of the snapshot at compaction start time instead of that of the newly produced snapshot
    rewrite-job-order	none	Force the rewrite job order based on the value.
      If rewrite-job-order=bytes-asc, then rewrite the smallest job groups first.
      If rewrite-job-order=bytes-desc, then rewrite the largest job groups first.
      If rewrite-job-order=files-asc, then rewrite the job groups with the least files first.
      If rewrite-job-order=files-desc, then rewrite the job groups with the most files first.
      If rewrite-job-order=none, then rewrite job groups in the order they were planned (no specific ordering).
    target-file-size-bytes	536870912 (512 MB, default value of write.target-file-size-bytes from table properties)	Target output file size
    min-file-size-bytes	75% of target file size	Files under this threshold will be considered for rewriting regardless of any other criteria
    max-file-size-bytes	180% of target file size	Files with sizes above this threshold will be considered for rewriting regardless of any other criteria
    min-input-files	5	Any file group with this number of files or more will be rewritten regardless of other criteria (the file group should have at least two files)
    rewrite-all	false	Force rewriting of all provided files overriding other options
    max-file-group-size-bytes	107374182400 (100GB)	Largest amount of data that should be rewritten in a single file group. The entire rewrite operation is broken down into pieces based on partitioning and within partitions based on size into file-groups. This helps with breaking down the rewriting of very large partitions which may not be rewritable otherwise due to the resource constraints of the cluster.
    delete-file-threshold	2147483647	Minimum number of deletes that needs to be associated with a data file for it to be considered for rewriting
    delete-ratio-threshold	0.3	Minimum deletion ratio that needs to be associated with a data file for it to be considered for rewriting
    output-spec-id	current partition spec id	Identifier of the output partition spec. Data will be reorganized during the rewrite to align with the output partitioning.
    remove-dangling-deletes	false	Remove dangling position and equality deletes after rewriting. A delete file is considered dangling if it does not apply to any live data files. Enabling this will generate an additional commit for the removal.
*/

sealed trait OptionsField[T] {
  def name: String
  def value: T
  def dependencies: List[OptionsField[Boolean]] = List()
  def writeValue: String = s"'$name', '$value'"

  def ofStrategies: Set[Class[? <: Strategy]]
}

trait CommonStrategies {
  def ofStrategies: Set[Class[? <: Strategy]]  = Set(classOf[Binpack], classOf[Sort], classOf[Zorder])
}

abstract class FieldWithName[T] protected (value: T) extends OptionsField[T] {
}

case class MaxConcurrentFileGroupRewrites(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "max-concurrent-file-group-rewrites"
}

case class PartialProgressEnabled(override val value: Boolean) extends FieldWithName[Boolean](value: Boolean) with CommonStrategies {
  override def name: String = "partial-progress.enabled"
}

case class PartialProgressEnabledCommits(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "partial-progress.max-commits"
  override def dependencies: List[OptionsField[Boolean]] = List(PartialProgressEnabled.asInstanceOf[OptionsField[Boolean]])
}

case class PartialProgressMaxFailedCommits(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "partial-progress.max-failed-commits"
  override def dependencies: List[OptionsField[Boolean]] = List(PartialProgressEnabled.asInstanceOf[OptionsField[Boolean]])
}

case class UseStartingSequenceNumber(override val value: Boolean) extends FieldWithName[Boolean](value: Boolean) with CommonStrategies {
  override def name: String = "use-starting-sequence-number"
}

case class RewriteJobOrder(override val value: RewriteJobOrderValues) extends FieldWithName[RewriteJobOrderValues](value: RewriteJobOrderValues) with CommonStrategies {
  override def name: String = "rewrite-job-order"
  override def writeValue: String = s"'$name', '${value.value}'"
}

case class TargetFileSizeBytes(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "target-file-size-bytes"
}

case class MinFileSizeBytes(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "min-file-size-bytes"
}

case class MaxFileSizeBytes(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "max-file-size-bytes"
}

case class MinInputFiles(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "min-input-files"
}

case class RewriteAll(override val value: Boolean) extends FieldWithName[Boolean](value: Boolean) with CommonStrategies {
  override def name: String = "rewrite-all"
}

case class MaxFileGroupSizeBytes(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "max-file-group-size-bytes"
}

case class DeleteFileThreshold(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "delete-file-threshold"
}

case class DeleteRatioThreshold(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "delete-ratio-threshold"
}

case class OutputSpecId(override val value: String) extends FieldWithName[String](value: String) with CommonStrategies {
  override def name: String = "output-spec-id"
}

case class RemoveDanglingDeletes(override val value: Boolean) extends FieldWithName[Boolean](value: Boolean) with CommonStrategies {
  override def name: String = "remove-dangling-deletes"
}

/*
Options for sort strategy
  compression-factor	1.0	The number of shuffle partitions and consequently the number of output files created by the Spark sort is based on the size of the input data files used in this file rewriter. Due to compression, the disk file sizes may not accurately represent the size of files in the output. This parameter lets the user adjust the file size used for estimating actual output data size. A factor greater than 1.0 would generate more files than we would expect based on the on-disk file size. A value less than 1.0 would create fewer files than we would expect based on the on-disk size.
  shuffle-partitions-per-file	1	Number of shuffle partitions to use for each output file. Iceberg will use a custom coalesce operation to stitch these sorted partitions back together into a single sorted file.
*/

case class CompressionFactor(override val value: Int) extends FieldWithName[Int](value: Int) {
  override def name: String = "compression-factor"
  override def ofStrategies: Set[Class[? <: Strategy]] = Set(classOf[Sort])
}

case class ShufflePartitionsPerFile(override val value: Int) extends FieldWithName[Int](value: Int) with CommonStrategies {
  override def name: String = "shuffle-partitions-per-file"
  override def ofStrategies: Set[Class[? <: Strategy]] = Set(classOf[Sort])
}

/*
Options for sort strategy with zorder sort_order🔗
  var-length-contribution	8	Number of bytes considered from an input column of a type with variable length (String, Binary)
  max-output-size	2147483647	Amount of bytes interleaved in the ZOrder algorithm
 */
case class VarLengthContribution(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "var-length-contribution"
  override def ofStrategies: Set[Class[? <: Strategy]] = Set(classOf[Sort], classOf[Zorder])
}
case class MaxOutputSize(override val value: Long) extends FieldWithName[Long](value: Long) with CommonStrategies {
  override def name: String = "max-output-size"
  override def ofStrategies: Set[Class[? <: Strategy]] = Set(classOf[Sort], classOf[Zorder])
}
