package com.bilibili.cluster.scheduler.common.dto.hadoop;

import cn.hutool.core.annotation.Alias;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @description: namenodefs的指标
 * @Date: 2024/4/28 11:10
 * @Author: nizhiqiang
 */
@NoArgsConstructor
@Data
public class NameNodeFsIndex {

    @Alias("name")
    private String name;
    @Alias("modelerType")
    private String modelerType;
    @Alias("tag.Context")
    private String tagContext;
    @Alias("tag.HAState")
    private String tagHaState;
    @Alias("tag.TotalSyncTimes")
    private String tagTotalSyncTimes;
    @Alias("tag.Hostname")
    private String tagHostname;
    @Alias("FSNWriteLockStartCommonServicesNanosNumOps")
    private Integer fSNWriteLockStartCommonServicesNanosNumOps;
    @Alias("FSNWriteLockStartCommonServicesNanosAvgTime")
    private Double fSNWriteLockStartCommonServicesNanosAvgTime;
    @Alias("FSNReadLockListStatusNanosNumOps")
    private Integer fSNReadLockListStatusNanosNumOps;
    @Alias("FSNReadLockListStatusNanosAvgTime")
    private Double fSNReadLockListStatusNanosAvgTime;
    @Alias("FSNWriteLockCompleteFileNanosNumOps")
    private Integer fSNWriteLockCompleteFileNanosNumOps;
    @Alias("FSNWriteLockCompleteFileNanosAvgTime")
    private Double fSNWriteLockCompleteFileNanosAvgTime;
    @Alias("FSNReadLockGetfileinfoNanosNumOps")
    private Integer fSNReadLockGetfileinfoNanosNumOps;
    @Alias("FSNReadLockGetfileinfoNanosAvgTime")
    private Double fSNReadLockGetfileinfoNanosAvgTime;
    @Alias("FSNWriteLockDeleteNanosNumOps")
    private Integer fSNWriteLockDeleteNanosNumOps;
    @Alias("FSNWriteLockDeleteNanosAvgTime")
    private Double fSNWriteLockDeleteNanosAvgTime;
    @Alias("FSNReadLockRenewLeaseNanosNumOps")
    private Integer fSNReadLockRenewLeaseNanosNumOps;
    @Alias("FSNReadLockRenewLeaseNanosAvgTime")
    private Double fSNReadLockRenewLeaseNanosAvgTime;
    @Alias("FSNWriteLockRenameNanosNumOps")
    private Integer fSNWriteLockRenameNanosNumOps;
    @Alias("FSNWriteLockRenameNanosAvgTime")
    private Double fSNWriteLockRenameNanosAvgTime;
    @Alias("FSNWriteLockClearCorruptLazyPersistFilesNanosNumOps")
    private Integer fSNWriteLockClearCorruptLazyPersistFilesNanosNumOps;
    @Alias("FSNWriteLockClearCorruptLazyPersistFilesNanosAvgTime")
    private Double fSNWriteLockClearCorruptLazyPersistFilesNanosAvgTime;
    @Alias("FSNWriteLockRollEditLogNanosNumOps")
    private Integer fSNWriteLockRollEditLogNanosNumOps;
    @Alias("FSNWriteLockRollEditLogNanosAvgTime")
    private Double fSNWriteLockRollEditLogNanosAvgTime;
    @Alias("FSNReadLockGetCompleteBlocksTotalNanosNumOps")
    private Integer fSNReadLockGetCompleteBlocksTotalNanosNumOps;
    @Alias("FSNReadLockGetCompleteBlocksTotalNanosAvgTime")
    private Double fSNReadLockGetCompleteBlocksTotalNanosAvgTime;
    @Alias("FSNWriteLockFsyncNanosNumOps")
    private Integer fSNWriteLockFsyncNanosNumOps;
    @Alias("FSNWriteLockFsyncNanosAvgTime")
    private Double fSNWriteLockFsyncNanosAvgTime;
    @Alias("FSNReadLockOTHERNanosNumOps")
    private Integer fSNReadLockOTHERNanosNumOps;
    @Alias("FSNReadLockOTHERNanosAvgTime")
    private Double fSNReadLockOTHERNanosAvgTime;
    @Alias("FSNReadLockListEncryptionZonesNanosNumOps")
    private Integer fSNReadLockListEncryptionZonesNanosNumOps;
    @Alias("FSNReadLockListEncryptionZonesNanosAvgTime")
    private Double fSNReadLockListEncryptionZonesNanosAvgTime;
    @Alias("FSNWriteLockCreateNanosNumOps")
    private Integer fSNWriteLockCreateNanosNumOps;
    @Alias("FSNWriteLockCreateNanosAvgTime")
    private Double fSNWriteLockCreateNanosAvgTime;
    @Alias("FSNWriteLockRefreshNodesNanosNumOps")
    private Integer fSNWriteLockRefreshNodesNanosNumOps;
    @Alias("FSNWriteLockRefreshNodesNanosAvgTime")
    private Double fSNWriteLockRefreshNodesNanosAvgTime;
    @Alias("FSNReadLockGetNamespaceInfoNanosNumOps")
    private Integer fSNReadLockGetNamespaceInfoNanosNumOps;
    @Alias("FSNReadLockGetNamespaceInfoNanosAvgTime")
    private Double fSNReadLockGetNamespaceInfoNanosAvgTime;
    @Alias("FSNWriteLockGetAdditionalBlockNanosNumOps")
    private Integer fSNWriteLockGetAdditionalBlockNanosNumOps;
    @Alias("FSNWriteLockGetAdditionalBlockNanosAvgTime")
    private Double fSNWriteLockGetAdditionalBlockNanosAvgTime;
    @Alias("FSNReadLockQueryRollingUpgradeNanosNumOps")
    private Integer fSNReadLockQueryRollingUpgradeNanosNumOps;
    @Alias("FSNReadLockQueryRollingUpgradeNanosAvgTime")
    private Double fSNReadLockQueryRollingUpgradeNanosAvgTime;
    @Alias("FSNWriteLockSetImageLoadedNanosNumOps")
    private Integer fSNWriteLockSetImageLoadedNanosNumOps;
    @Alias("FSNWriteLockSetImageLoadedNanosAvgTime")
    private Double fSNWriteLockSetImageLoadedNanosAvgTime;
    @Alias("FSNWriteLockDoTailEditsNanosNumOps")
    private Integer fSNWriteLockDoTailEditsNanosNumOps;
    @Alias("FSNWriteLockDoTailEditsNanosAvgTime")
    private Double fSNWriteLockDoTailEditsNanosAvgTime;
    @Alias("FSNReadLockDatanodeReportNanosNumOps")
    private Integer fSNReadLockDatanodeReportNanosNumOps;
    @Alias("FSNReadLockDatanodeReportNanosAvgTime")
    private Double fSNReadLockDatanodeReportNanosAvgTime;
    @Alias("FSNWriteLockLeaseManagerNanosNumOps")
    private Integer fSNWriteLockLeaseManagerNanosNumOps;
    @Alias("FSNWriteLockLeaseManagerNanosAvgTime")
    private Double fSNWriteLockLeaseManagerNanosAvgTime;
    @Alias("FSNReadLockGetAdditionalBlockNanosNumOps")
    private Integer fSNReadLockGetAdditionalBlockNanosNumOps;
    @Alias("FSNReadLockGetAdditionalBlockNanosAvgTime")
    private Double fSNReadLockGetAdditionalBlockNanosAvgTime;
    @Alias("FSNWriteLockMkdirsNanosNumOps")
    private Integer fSNWriteLockMkdirsNanosNumOps;
    @Alias("FSNWriteLockMkdirsNanosAvgTime")
    private Double fSNWriteLockMkdirsNanosAvgTime;
    @Alias("FSNWriteLockOTHERNanosNumOps")
    private Integer fSNWriteLockOTHERNanosNumOps;
    @Alias("FSNWriteLockOTHERNanosAvgTime")
    private Double fSNWriteLockOTHERNanosAvgTime;
    @Alias("FSNWriteLockLoadFSImageNanosNumOps")
    private Integer fSNWriteLockLoadFSImageNanosNumOps;
    @Alias("FSNWriteLockLoadFSImageNanosAvgTime")
    private Double fSNWriteLockLoadFSImageNanosAvgTime;
    @Alias("BMWriteLockOverallNanosNumOps")
    private Integer bMWriteLockOverallNanosNumOps;
    @Alias("BMWriteLockOverallNanosAvgTime")
    private Double bMWriteLockOverallNanosAvgTime;
    @Alias("BMWriteLockRemoveBRLeaseIfNeededNanosNumOps")
    private Integer bMWriteLockRemoveBRLeaseIfNeededNanosNumOps;
    @Alias("BMWriteLockRemoveBRLeaseIfNeededNanosAvgTime")
    private Double bMWriteLockRemoveBRLeaseIfNeededNanosAvgTime;
    @Alias("BMWriteLockLoadFSImageNanosNumOps")
    private Integer bMWriteLockLoadFSImageNanosNumOps;
    @Alias("BMWriteLockLoadFSImageNanosAvgTime")
    private Double bMWriteLockLoadFSImageNanosAvgTime;
    @Alias("BMWriteLockStartActiveServicesNanosNumOps")
    private Integer bMWriteLockStartActiveServicesNanosNumOps;
    @Alias("BMWriteLockStartActiveServicesNanosAvgTime")
    private Double bMWriteLockStartActiveServicesNanosAvgTime;
    @Alias("BMWriteLockInvalidateWorkForOneNodeNanosNumOps")
    private Integer bMWriteLockInvalidateWorkForOneNodeNanosNumOps;
    @Alias("BMWriteLockInvalidateWorkForOneNodeNanosAvgTime")
    private Double bMWriteLockInvalidateWorkForOneNodeNanosAvgTime;
    @Alias("BMWriteLockCompleteFileNanosNumOps")
    private Integer bMWriteLockCompleteFileNanosNumOps;
    @Alias("BMWriteLockCompleteFileNanosAvgTime")
    private Double bMWriteLockCompleteFileNanosAvgTime;
    @Alias("BMWriteLockLoadEditRecordsNanosNumOps")
    private Integer bMWriteLockLoadEditRecordsNanosNumOps;
    @Alias("BMWriteLockLoadEditRecordsNanosAvgTime")
    private Double bMWriteLockLoadEditRecordsNanosAvgTime;
    @Alias("BMReadLockOverallNanosNumOps")
    private Long bMReadLockOverallNanosNumOps;
    @Alias("BMReadLockOverallNanosAvgTime")
    private Double bMReadLockOverallNanosAvgTime;
    @Alias("BMWriteLockGetAdditionalBlockNanosNumOps")
    private Integer bMWriteLockGetAdditionalBlockNanosNumOps;
    @Alias("BMWriteLockGetAdditionalBlockNanosAvgTime")
    private Double bMWriteLockGetAdditionalBlockNanosAvgTime;
    @Alias("BMWriteLockProcessQueueNanosNumOps")
    private Integer bMWriteLockProcessQueueNanosNumOps;
    @Alias("BMWriteLockProcessQueueNanosAvgTime")
    private Double bMWriteLockProcessQueueNanosAvgTime;
    @Alias("BMWriteLockSafeModeMonitorNanosNumOps")
    private Integer bMWriteLockSafeModeMonitorNanosNumOps;
    @Alias("BMWriteLockSafeModeMonitorNanosAvgTime")
    private Double bMWriteLockSafeModeMonitorNanosAvgTime;
    @Alias("BMWriteLockRescanPostponedMisreplicatedBlocksNanosNumOps")
    private Integer bMWriteLockRescanPostponedMisreplicatedBlocksNanosNumOps;
    @Alias("BMWriteLockRescanPostponedMisreplicatedBlocksNanosAvgTime")
    private Double bMWriteLockRescanPostponedMisreplicatedBlocksNanosAvgTime;
    @Alias("BMWriteLockDecommissionManagerProcessPendingNodesNanosNumOps")
    private Integer bMWriteLockDecommissionManagerProcessPendingNodesNanosNumOps;
    @Alias("BMWriteLockDecommissionManagerProcessPendingNodesNanosAvgTime")
    private Double bMWriteLockDecommissionManagerProcessPendingNodesNanosAvgTime;
    @Alias("BMWriteLockValidateReplicationWorkNanosNumOps")
    private Integer bMWriteLockValidateReplicationWorkNanosNumOps;
    @Alias("BMWriteLockValidateReplicationWorkNanosAvgTime")
    private Double bMWriteLockValidateReplicationWorkNanosAvgTime;
    @Alias("BMWriteLockFsyncNanosNumOps")
    private Integer bMWriteLockFsyncNanosNumOps;
    @Alias("BMWriteLockFsyncNanosAvgTime")
    private Double bMWriteLockFsyncNanosAvgTime;
    @Alias("BMWriteLockScheduleReplicationWorkNanosNumOps")
    private Integer bMWriteLockScheduleReplicationWorkNanosNumOps;
    @Alias("BMWriteLockScheduleReplicationWorkNanosAvgTime")
    private Double bMWriteLockScheduleReplicationWorkNanosAvgTime;
    @Alias("BMWriteLockRegisterDatanodeNanosNumOps")
    private Integer bMWriteLockRegisterDatanodeNanosNumOps;
    @Alias("BMWriteLockRegisterDatanodeNanosAvgTime")
    private Double bMWriteLockRegisterDatanodeNanosAvgTime;
    @Alias("BMWriteLockRescanCachedBlockMapNanosNumOps")
    private Integer bMWriteLockRescanCachedBlockMapNanosNumOps;
    @Alias("BMWriteLockRescanCachedBlockMapNanosAvgTime")
    private Double bMWriteLockRescanCachedBlockMapNanosAvgTime;
    @Alias("BMWriteLockClearCorruptLazyPersistFilesNanosNumOps")
    private Integer bMWriteLockClearCorruptLazyPersistFilesNanosNumOps;
    @Alias("BMWriteLockClearCorruptLazyPersistFilesNanosAvgTime")
    private Double bMWriteLockClearCorruptLazyPersistFilesNanosAvgTime;
    @Alias("BMWriteLockComputeReplicationWorkNanosNumOps")
    private Integer bMWriteLockComputeReplicationWorkNanosNumOps;
    @Alias("BMWriteLockComputeReplicationWorkNanosAvgTime")
    private Double bMWriteLockComputeReplicationWorkNanosAvgTime;
    @Alias("BMReadLockHandleHeartbeatNanosNumOps")
    private Long bMReadLockHandleHeartbeatNanosNumOps;
    @Alias("BMReadLockHandleHeartbeatNanosAvgTime")
    private Double bMReadLockHandleHeartbeatNanosAvgTime;
    @Alias("BMWriteLockComputeDatanodeWorkNanosNumOps")
    private Integer bMWriteLockComputeDatanodeWorkNanosNumOps;
    @Alias("BMWriteLockComputeDatanodeWorkNanosAvgTime")
    private Double bMWriteLockComputeDatanodeWorkNanosAvgTime;
    @Alias("BMWriteLockRemoveDeadDatanodeNanosNumOps")
    private Integer bMWriteLockRemoveDeadDatanodeNanosNumOps;
    @Alias("BMWriteLockRemoveDeadDatanodeNanosAvgTime")
    private Double bMWriteLockRemoveDeadDatanodeNanosAvgTime;
    @Alias("BMWriteLockDeleteNanosNumOps")
    private Integer bMWriteLockDeleteNanosNumOps;
    @Alias("BMWriteLockDeleteNanosAvgTime")
    private Double bMWriteLockDeleteNanosAvgTime;
    @Alias("BMWriteLockRemoveBlockNanosNumOps")
    private Integer bMWriteLockRemoveBlockNanosNumOps;
    @Alias("BMWriteLockRemoveBlockNanosAvgTime")
    private Double bMWriteLockRemoveBlockNanosAvgTime;
    @Alias("BMWriteLockStartCommonServicesNanosNumOps")
    private Integer bMWriteLockStartCommonServicesNanosNumOps;
    @Alias("BMWriteLockStartCommonServicesNanosAvgTime")
    private Double bMWriteLockStartCommonServicesNanosAvgTime;
    @Alias("MissingBlocks")
    private Integer missingBlocks;
    @Alias("MissingReplOneBlocks")
    private Integer missingReplOneBlocks;
    @Alias("ExpiredHeartbeats")
    private Integer expiredHeartbeats;
    @Alias("TransactionsSinceLastCheckpoint")
    private Integer transactionsSinceLastCheckpoint;
    @Alias("TransactionsSinceLastLogRoll")
    private Integer transactionsSinceLastLogRoll;
    @Alias("LastWrittenTransactionId")
    private Long lastWrittenTransactionId;
    @Alias("LastCheckpointTime")
    private Long lastCheckpointTime;
    @Alias("CapacityTotal")
    private Long capacityTotal;
    @Alias("CapacityTotalGB")
    private Double capacityTotalGB;
    @Alias("CapacityUsed")
    private Long capacityUsed;
    @Alias("CapacityUsedGB")
    private Double capacityUsedGB;
    @Alias("CapacityRemaining")
    private Long capacityRemaining;
    @Alias("CapacityRemainingGB")
    private Double capacityRemainingGB;
    @Alias("CapacityUsedNonDFS")
    private Long capacityUsedNonDFS;
    @Alias("TotalLoad")
    private Integer totalLoad;
    @Alias("SnapshottableDirectories")
    private Integer snapshottableDirectories;
    @Alias("Snapshots")
    private Integer snapshots;
    @Alias("NumEncryptionZones")
    private Integer numEncryptionZones;
    @Alias("CurrentDeadLockCount")
    private Integer currentDeadLockCount;
    @Alias("LockQueueLength")
    private Integer lockQueueLength;
    @Alias("WriteLockCount")
    private Integer writeLockCount;
    @Alias("ReadLockCount")
    private Integer readLockCount;
    @Alias("BlocksTotal")
    private Integer blocksTotal;
    @Alias("NumFilesUnderConstruction")
    private Integer numFilesUnderConstruction;
    @Alias("NumActiveClients")
    private Integer numActiveClients;
    @Alias("FilesTotal")
    private Integer filesTotal;
    @Alias("PendingReplicationBlocks")
    private Integer pendingReplicationBlocks;
    @Alias("UnderReplicatedBlocks")
    private Integer underReplicatedBlocks;
    @Alias("CorruptBlocks")
    private Integer corruptBlocks;
    @Alias("CorruptionReportedReplicas")
    private Integer corruptionReportedReplicas;
    @Alias("ScheduledReplicationBlocks")
    private Integer scheduledReplicationBlocks;
    @Alias("PendingDeletionBlocks")
    private Integer pendingDeletionBlocks;
    @Alias("ExcessBlocks")
    private Integer excessBlocks;
    @Alias("NumTimedOutPendingReplications")
    private Integer numTimedOutPendingReplications;
    @Alias("PostponedMisreplicatedBlocks")
    private Integer postponedMisreplicatedBlocks;
    @Alias("PendingDataNodeMessageCount")
    private Integer pendingDataNodeMessageCount;
    @Alias("MillisSinceLastLoadedEdits")
    private Integer millisSinceLastLoadedEdits;
    @Alias("BlockCapacity")
    private Integer blockCapacity;
    @Alias("NumLiveDataNodes")
    private Integer numLiveDataNodes;
    @Alias("NumDeadDataNodes")
    private Integer numDeadDataNodes;
    @Alias("NumDecomLiveDataNodes")
    private Integer numDecomLiveDataNodes;
    @Alias("NumDecomDeadDataNodes")
    private Integer numDecomDeadDataNodes;
    @Alias("VolumeFailuresTotal")
    private Integer volumeFailuresTotal;
    @Alias("EstimatedCapacityLostTotal")
    private Long estimatedCapacityLostTotal;
    @Alias("NumDecommissioningDataNodes")
    private Integer numDecommissioningDataNodes;
    @Alias("StaleDataNodes")
    private Integer staleDataNodes;
    @Alias("NumStaleStorages")
    private Integer numStaleStorages;
    @Alias("TotalFiles")
    private Integer totalFiles;
    @Alias("TotalSyncCount")
    private Integer totalSyncCount;
}
