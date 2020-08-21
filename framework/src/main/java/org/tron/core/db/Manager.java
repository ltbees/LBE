package org.un.core.db;

import static org.un.common.utils.Commons.adjustBalance;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Longs;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeLbeit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import javax.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.spongycastle.util.encoders.Hex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.un.common.args.GenesisBlock;
import org.un.common.logsfilter.EventPluginLoader;
import org.un.common.logsfilter.FilterQuery;
import org.un.common.logsfilter.capsule.BlockLogTriggerCapsule;
import org.un.common.logsfilter.capsule.ContractEventTriggerCapsule;
import org.un.common.logsfilter.capsule.ContractLogTriggerCapsule;
import org.un.common.logsfilter.capsule.ContractTriggerCapsule;
import org.un.common.logsfilter.capsule.SolidityTriggerCapsule;
import org.un.common.logsfilter.capsule.TransactionLogTriggerCapsule;
import org.un.common.logsfilter.capsule.TriggerCapsule;
import org.un.common.logsfilter.trigger.ContractEventTrigger;
import org.un.common.logsfilter.trigger.ContractLogTrigger;
import org.un.common.logsfilter.trigger.ContractTrigger;
import org.un.common.logsfilter.trigger.Trigger;
import org.un.common.overlay.message.Message;
import org.un.common.parameter.CommonParameter;
import org.un.common.runtime.RuntimeImpl;
import org.un.common.utils.ByteArray;
import org.un.common.utils.ForkController;
import org.un.common.utils.Pair;
import org.un.common.utils.SessionOptional;
import org.un.common.utils.Sha256Hash;
import org.un.common.zksnark.MerkleContainer;
import org.un.consensus.Consensus;
import org.un.consensus.base.Param.Miner;
import org.un.core.ChainBaseManager;
import org.un.core.Constant;
import org.un.core.actuator.ActuatorCreator;
import org.un.core.capsule.AccountCapsule;
import org.un.core.capsule.BlockCapsule;
import org.un.core.capsule.BlockCapsule.BlockId;
import org.un.core.capsule.BytesCapsule;
import org.un.core.capsule.TransactionCapsule;
import org.un.core.capsule.TransactionInfoCapsule;
import org.un.core.capsule.TransactionRetCapsule;
import org.un.core.capsule.WitnessCapsule;
import org.un.core.capsule.utils.TransactionUtil;
import org.un.core.config.Parameter.ChainConstant;
import org.un.core.config.args.Args;
import org.un.core.consensus.ProposalController;
import org.un.core.db.KhaosDatabase.KhaosBlock;
import org.un.core.db.accountstate.TrieService;
import org.un.core.db.accountstate.callback.AccountStateCallBack;
import org.un.core.db.api.AssetUpdateHelper;
import org.un.core.db2.ISession;
import org.un.core.db2.core.Chainbase;
import org.un.core.db2.core.ILbeChainBase;
import org.un.core.db2.core.SnapshotManager;
import org.un.core.exception.AccountResourceInsufficientException;
import org.un.core.exception.BadBlockException;
import org.un.core.exception.BadItemException;
import org.un.core.exception.BadNumberBlockException;
import org.un.core.exception.BalanceInsufficientException;
import org.un.core.exception.ContractExeException;
import org.un.core.exception.ContractSizeNotEqualToOneException;
import org.un.core.exception.ContractValidateException;
import org.un.core.exception.DupTransactionException;
import org.un.core.exception.ItemNotFoundException;
import org.un.core.exception.NonCommonBlockException;
import org.un.core.exception.ReceiptCheckErrException;
import org.un.core.exception.TaposException;
import org.un.core.exception.TooBigTransactionException;
import org.un.core.exception.TooBigTransactionResultException;
import org.un.core.exception.TransactionExpirationException;
import org.un.core.exception.LbeLinkedBlockException;
import org.un.core.exception.VMIllegalException;
import org.un.core.exception.ValidateScheduleException;
import org.un.core.exception.ValidateSignatureException;
import org.un.core.exception.ZksnarkException;
import org.un.core.metrics.MetricsKey;
import org.un.core.metrics.MetricsUtil;
import org.un.core.store.AccountIdIndexStore;
import org.un.core.store.AccountIndexStore;
import org.un.core.store.AccountStore;
import org.un.core.store.AssetIssueStore;
import org.un.core.store.AssetIssueV2Store;
import org.un.core.store.CodeStore;
import org.un.core.store.ContractStore;
import org.un.core.store.DelegatedResourceAccountIndexStore;
import org.un.core.store.DelegatedResourceStore;
import org.un.core.store.DelegationStore;
import org.un.core.store.DynamicPropertiesStore;
import org.un.core.store.ExchangeStore;
import org.un.core.store.ExchangeV2Store;
import org.un.core.store.IncrementalMerkleTreeStore;
import org.un.core.store.NullifierStore;
import org.un.core.store.ProposalStore;
import org.un.core.store.StorageRowStore;
import org.un.core.store.StoreFactory;
import org.un.core.store.TransactionHistoryStore;
import org.un.core.store.TransactionRetStore;
import org.un.core.store.TreeBlockIndexStore;
import org.un.core.store.VotesStore;
import org.un.core.store.WitnessScheduleStore;
import org.un.core.store.WitnessStore;
import org.un.core.utils.TransactionRegister;
import org.un.protos.Protocol.AccountType;
import org.un.protos.Protocol.Transaction;
import org.un.protos.Protocol.Transaction.Contract;
import org.un.protos.Protocol.TransactionInfo;


@Slf4j(topic = "DB")
@Component
public class Manager {

  private static final int SHIELDED_TRANS_IN_BLOCK_COLBETS = 1;
  private static final String SAVE_BLOCK = "save block: ";
  private final int shieldedTransInPendingMaxCounts =
      Args.getInstance().getShieldedTransInPendingMaxCounts();
  private int maxTransactionPendingSize = Args.getInstance().getMaxTransactionPendingSize();
  @Getter
  @Setter
  public boolean eventPluginLoaded = false;
  @Autowired(required = false)
  @Getter
  private TransactionCache transactionCache;
  @Autowired
  private KhaosDatabase khaosDb;
  @Getter
  @Autowired
  private RevokingDatabase revokingStore;
  @Getter
  private SessionOptional session = SessionOptional.instance();
  @Getter
  @Setter
  private boolean isSyncMode;

  // map<Long, IncrementalMerkleTree>
  @Getter
  @Setter
  private String netType;
  @Getter
  @Setter
  private ProposalController proposalController;
  @Getter
  @Setter
  private MerkleContainer merkleContainer;
  @Autowired
  @Getter
  @Setter
  private TreeBlockIndexStore merkleTreeIndexStore;
  private ExecutorService validateSignService;
  private boolean isRunRePushThread = true;
  private boolean isRunTriggerCapsuleProcessThread = true;
  private BlockingQueue<TransactionCapsule> pushTransactionQueue = new LinkedBlockingQueue<>();
  @Getter
  private Cache<Sha256Hash, Boolean> transactionIdCache = CacheBuilder
      .newBuilder().maximumSize(100_000).recordStats().build();
  @Getter
  private ForkController forkController = ForkController.instance();
  @Autowired
  private AccountStateCallBack accountStateCallBack;
  @Autowired
  private TrieService trieService;
  private Set<String> ownerAddressSet = new HashSet<>();
  @Getter
  @Autowired
  private DelegationService delegationService;
  @Autowired
  private Consensus consensus;
  @Autowired
  @Getter
  private ChainBaseManager chainBaseManager;
  // transactions cache
  private List<TransactionCapsule> pendingTransactions;
  @Getter
  private AtomicInteger shieldedTransInPendingCounts = new AtomicInteger(0);
  // transactions popped
  private List<TransactionCapsule> poppedTransactions =
      Collections.synchronizedList(Lists.newArrayList());
  // the capacity is equal to Integer.MAX_VALUE default
  private BlockingQueue<TransactionCapsule> rePushTransactions;
  private BlockingQueue<TriggerCapsule> triggerCapsuleQueue;

  /**
   * Cycle thread to rePush Transactions
   */
  private Runnable rePushLoop =
      () -> {
        while (isRunRePushThread) {
          TransactionCapsule tx = null;
          try {
            tx = getRePushTransactions().peek();
            if (tx != null) {
              this.rePush(tx);
            } else {
              TimeLbeit.MILLISECONDS.sleep(50L);
            }
          } catch (Exception ex) {
            logger.error("unknown exception happened in rePush loop", ex);
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in rePush loop", throwable);
          } finally {
            if (tx != null) {
              getRePushTransactions().remove(tx);
            }
          }
        }
      };
  private Runnable triggerCapsuleProcessLoop =
      () -> {
        while (isRunTriggerCapsuleProcessThread) {
          try {
            TriggerCapsule triggerCapsule = triggerCapsuleQueue.poll(1, TimeLbeit.SECONDS);
            if (triggerCapsule != null) {
              triggerCapsule.processTrigger();
            }
          } catch (InterruptedException ex) {
            logger.info(ex.getMessage());
            Thread.currentThread().interrupt();
          } catch (Exception ex) {
            logger.error("unknown exception happened in process capsule loop", ex);
          } catch (Throwable throwable) {
            logger.error("unknown throwable happened in process capsule loop", throwable);
          }
        }
      };

  public WitnessStore getWitnessStore() {
    return chainBaseManager.getWitnessStore();
  }

  public boolean needToUpdateAsset() {
    return getDynamicPropertiesStore().getTokenUpdateDone() == 0L;
  }

  public DynamicPropertiesStore getDynamicPropertiesStore() {
    return chainBaseManager.getDynamicPropertiesStore();
  }

  public DelegationStore getDelegationStore() {
    return chainBaseManager.getDelegationStore();
  }

  public IncrementalMerkleTreeStore getMerkleTreeStore() {
    return chainBaseManager.getMerkleTreeStore();
  }

  public WitnessScheduleStore getWitnessScheduleStore() {
    return chainBaseManager.getWitnessScheduleStore();
  }

  public DelegatedResourceStore getDelegatedResourceStore() {
    return chainBaseManager.getDelegatedResourceStore();
  }

  public DelegatedResourceAccountIndexStore getDelegatedResourceAccountIndexStore() {
    return chainBaseManager.getDelegatedResourceAccountIndexStore();
  }

  public CodeStore getCodeStore() {
    return chainBaseManager.getCodeStore();
  }

  public ContractStore getContractStore() {
    return chainBaseManager.getContractStore();
  }

  public VotesStore getVotesStore() {
    return chainBaseManager.getVotesStore();
  }

  public ProposalStore getProposalStore() {
    return chainBaseManager.getProposalStore();
  }

  public ExchangeStore getExchangeStore() {
    return chainBaseManager.getExchangeStore();
  }

  public ExchangeV2Store getExchangeV2Store() {
    return chainBaseManager.getExchangeV2Store();
  }

  public StorageRowStore getStorageRowStore() {
    return chainBaseManager.getStorageRowStore();
  }

  public BlockIndexStore getBlockIndexStore() {
    return chainBaseManager.getBlockIndexStore();
  }

  public List<TransactionCapsule> getPendingTransactions() {
    return this.pendingTransactions;
  }

  public List<TransactionCapsule> getPoppedTransactions() {
    return this.poppedTransactions;
  }

  public BlockingQueue<TransactionCapsule> getRePushTransactions() {
    return rePushTransactions;
  }

  public void stopRePushThread() {
    isRunRePushThread = false;
  }

  public void stopRePushTriggerThread() {
    isRunTriggerCapsuleProcessThread = false;
  }

  @PostConstruct
  public void init() {
    Message.setDynamicPropertiesStore(this.getDynamicPropertiesStore());
    delegationService
        .initStore(chainBaseManager.getWitnessStore(), chainBaseManager.getDelegationStore(),
            chainBaseManager.getDynamicPropertiesStore(), chainBaseManager.getAccountStore());
    accountStateCallBack.setChainBaseManager(chainBaseManager);
    trieService.setChainBaseManager(chainBaseManager);
    revokingStore.disable();
    revokingStore.check();
    this.setProposalController(ProposalController.createInstance(this));
    this.setMerkleContainer(
        merkleContainer.createInstance(chainBaseManager.getMerkleTreeStore(),
            this.merkleTreeIndexStore));
    this.pendingTransactions = Collections.synchronizedList(Lists.newArrayList());
    this.rePushTransactions = new LinkedBlockingQueue<>();
    this.triggerCapsuleQueue = new LinkedBlockingQueue<>();
    chainBaseManager.setMerkleContainer(getMerkleContainer());
    chainBaseManager.setDelegationService(delegationService);

    this.initGenesis();
    try {
      this.khaosDb.start(chainBaseManager.getBlockById(
          getDynamicPropertiesStore().getLatestBlockHeaderHash()));
    } catch (ItemNotFoundException e) {
      logger.error(
          "Can not find Dynamic highest block from DB! \nnumber={} \nhash={}",
          getDynamicPropertiesStore().getLatestBlockHeaderNumber(),
          getDynamicPropertiesStore().getLatestBlockHeaderHash());
      logger.error(
          "Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    } catch (BadItemException e) {
      logger.error("DB data broken! {}", e);
      logger.error(
          "Please delete database directory({}) and restart",
          Args.getInstance().getOutputDirectory());
      System.exit(1);
    }
    forkController.init(this.chainBaseManager);

    if (Args.getInstance().isNeedToUpdateAsset() && needToUpdateAsset()) {
      new AssetUpdateHelper(chainBaseManager).doWork();
    }

    //for test only
    chainBaseManager.getDynamicPropertiesStore().updateDynamicStoreByConfig();

    initCacheTxs();
    revokingStore.enable();
    validateSignService = Executors
        .newFixedThreadPool(Args.getInstance().getValidateSignThreadNum());
    Thread rePushThread = new Thread(rePushLoop);
    rePushThread.start();
    // add contract event listener for subscribing
    if (Args.getInstance().isEventSubscribe()) {
      startEventSubscribing();
      Thread triggerCapsuleProcessThread = new Thread(triggerCapsuleProcessLoop);
      triggerCapsuleProcessThread.start();
    }

    //initStoreFactory
    prepareStoreFactory();
    //initActuatorCreator
    ActuatorCreator.init();
    TransactionRegister.registerActuator();
  }

  /**
   * init genesis block.
   */
  public void initGenesis() {
    chainBaseManager.initGenesis();
    BlockCapsule genesisBlock = chainBaseManager.getGenesisBlock();

    if (chainBaseManager.containBlock(genesisBlock.getBlockId())) {
      Args.getInstance().setChainId(genesisBlock.getBlockId().toString());
    } else {
      if (chainBaseManager.hasBlocks()) {
        logger.error(
            "genesis block modify, please delete database directory({}) and restart",
            Args.getInstance().getOutputDirectory());
        System.exit(1);
      } else {
        logger.info("create genesis block");
        Args.getInstance().setChainId(genesisBlock.getBlockId().toString());

        chainBaseManager.getBlockStore().put(genesisBlock.getBlockId().getBytes(), genesisBlock);
        chainBaseManager.getBlockIndexStore().put(genesisBlock.getBlockId());

        logger.info(SAVE_BLOCK + genesisBlock);
        // init Dynamic Properties Store
        chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderNumber(0);
        chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderHash(
            genesisBlock.getBlockId().getByteString());
        chainBaseManager.getDynamicPropertiesStore().saveLatestBlockHeaderTimestamp(
            genesisBlock.getTimeStamp());
        this.initAccount();
        this.initWitness();
        this.khaosDb.start(genesisBlock);
        this.updateRecentBlock(genesisBlock);
      }
    }
  }

  /**
   * save account into database.
   */
  public void initAccount() {
    final CommonParameter parameter = CommonParameter.getInstance();
    final GenesisBlock genesisBlockArg = parameter.getGenesisBlock();
    genesisBlockArg
        .getAssets()
        .forEach(
            account -> {
              account.setAccountType("Normal"); // to be set in conf
              final AccountCapsule accountCapsule =
                  new AccountCapsule(
                      account.getAccountName(),
                      ByteString.copyFrom(account.getAddress()),
                      account.getAccountType(),
                      account.getBalance());
              chainBaseManager.getAccountStore().put(account.getAddress(), accountCapsule);
              chainBaseManager.getAccountIdIndexStore().put(accountCapsule);
              chainBaseManager.getAccountIndexStore().put(accountCapsule);
            });
  }

  /**
   * save witnesses into database.
   */
  private void initWitness() {
    final CommonParameter commonParameter = Args.getInstance();
    final GenesisBlock genesisBlockArg = commonParameter.getGenesisBlock();
    genesisBlockArg
        .getWitnesses()
        .forEach(
            key -> {
              byte[] keyAddress = key.getAddress();
              ByteString address = ByteString.copyFrom(keyAddress);

              final AccountCapsule accountCapsule;
              if (!chainBaseManager.getAccountStore().has(keyAddress)) {
                accountCapsule = new AccountCapsule(ByteString.EMPTY,
                    address, AccountType.AssetIssue, 0L);
              } else {
                accountCapsule = chainBaseManager.getAccountStore().getLbechecked(keyAddress);
              }
              accountCapsule.setIsWitness(true);
              chainBaseManager.getAccountStore().put(keyAddress, accountCapsule);

              final WitnessCapsule witnessCapsule =
                  new WitnessCapsule(address, key.getVoteCount(), key.getUrl());
              witnessCapsule.setIsJobs(true);
              chainBaseManager.getWitnessStore().put(keyAddress, witnessCapsule);
            });
  }

  public void initCacheTxs() {
    logger.info("begin to init txs cache.");
    int dbVersion = Args.getInstance().getStorage().getDbVersion();
    if (dbVersion != 2) {
      return;
    }
    long start = System.currentTimeMillis();
    long headNum = chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber();
    long recentBlockCount = chainBaseManager.getRecentBlockStore().size();
    ListeningExecutorService service = MoreExecutors
        .listeningDecorator(Executors.newFixedThreadPool(50));
    List<ListenableFuture<?>> futures = new ArrayList<>();
    AtomicLong blockCount = new AtomicLong(0);
    AtomicLong emptyBlockCount = new AtomicLong(0);
    LongStream.rangeClosed(headNum - recentBlockCount + 1, headNum).forEach(
        blockNum -> futures.add(service.submit(() -> {
          try {
            blockCount.incrementAndGet();
            if (chainBaseManager.getBlockByNum(blockNum).getTransactions().isEmpty()) {
              emptyBlockCount.incrementAndGet();
            }
            chainBaseManager.getBlockByNum(blockNum).getTransactions().stream()
                .map(tc -> tc.getTransactionId().getBytes())
                .map(bytes -> Maps.immutableEntry(bytes, Longs.toByteArray(blockNum)))
                .forEach(e -> transactionCache
                    .put(e.getKey(), new BytesCapsule(e.getValue())));
          } catch (ItemNotFoundException | BadItemException e) {
            logger.info("init txs cache error.");
            throw new IllegalStateException("init txs cache error.");
          }
        })));

    ListenableFuture<?> future = Futures.allAsList(futures);
    try {
      future.get();
      service.shutdown();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } catch (ExecutionException e) {
      logger.info(e.getMessage());
    }

    logger.info("end to init txs cache. trx ids:{}, block count:{}, empty block count:{}, cost:{}",
        transactionCache.size(),
        blockCount.get(),
        emptyBlockCount.get(),
        System.currentTimeMillis() - start
    );
  }

  public AccountStore getAccountStore() {
    return chainBaseManager.getAccountStore();
  }

  public AccountIndexStore getAccountIndexStore() {
    return chainBaseManager.getAccountIndexStore();
  }

  void validateTapos(TransactionCapsule transactionCapsule) throws TaposException {
    byte[] refBlockHash = transactionCapsule.getInstance()
        .getRawData().getRefBlockHash().toByteArray();
    byte[] refBlockNumBytes = transactionCapsule.getInstance()
        .getRawData().getRefBlockBytes().toByteArray();
    try {
      byte[] blockHash = chainBaseManager.getRecentBlockStore().get(refBlockNumBytes).getData();
      if (!Arrays.equals(blockHash, refBlockHash)) {
        String str = String.format(
            "Tapos failed, different block hash, %s, %s , recent block %s, "
                + "solid block %s head block %s",
            ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
            Hex.toHexString(blockHash),
            chainBaseManager.getSolidBlockId().getString(),
            chainBaseManager.getHeadBlockId().getString()).toString();
        logger.info(str);
        throw new TaposException(str);
      }
    } catch (ItemNotFoundException e) {
      String str = String
          .format("Tapos failed, block not found, ref block %s, %s , solid block %s head block %s",
              ByteArray.toLong(refBlockNumBytes), Hex.toHexString(refBlockHash),
              chainBaseManager.getSolidBlockId().getString(),
              chainBaseManager.getHeadBlockId().getString()).toString();
      logger.info(str);
      throw new TaposException(str);
    }
  }

  void validateCommon(TransactionCapsule transactionCapsule)
      throws TransactionExpirationException, TooBigTransactionException {
    if (transactionCapsule.getData().length > Constant.TRANSACTION_MAX_BYTE_SIZE) {
      throw new TooBigTransactionException(
          "too big transaction, the size is " + transactionCapsule.getData().length + " bytes");
    }
    long transactionExpiration = transactionCapsule.getExpiration();
    long headBlockTime = chainBaseManager.getHeadBlockTimeStamp();
    if (transactionExpiration <= headBlockTime
        || transactionExpiration > headBlockTime + Constant.MAXIMUM_TIME_LBETIL_EXPIRATION) {
      throw new TransactionExpirationException(
          "transaction expiration, transaction expiration time is " + transactionExpiration
              + ", but headBlockTime is " + headBlockTime);
    }
  }

  void validateDup(TransactionCapsule transactionCapsule) throws DupTransactionException {
    if (containsTransaction(transactionCapsule)) {
      logger.debug(ByteArray.toHexString(transactionCapsule.getTransactionId().getBytes()));
      throw new DupTransactionException("dup trans");
    }
  }

  private boolean containsTransaction(TransactionCapsule transactionCapsule) {
    if (transactionCache != null) {
      return transactionCache.has(transactionCapsule.getTransactionId().getBytes());
    }

    return chainBaseManager.getTransactionStore()
        .has(transactionCapsule.getTransactionId().getBytes());
  }

  /**
   * push transaction into pending.
   */
  public boolean pushTransaction(final TransactionCapsule trx)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, DupTransactionException, TaposException,
      TooBigTransactionException, TransactionExpirationException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException {

    if (isShieldedTransaction(trx.getInstance()) && !Args.getInstance()
        .isFullNodeAllowShieldedTransactionArgs()) {
      return true;
    }

    pushTransactionQueue.add(trx);

    try {
      if (!trx.validateSignature(chainBaseManager.getAccountStore(),
          chainBaseManager.getDynamicPropertiesStore())) {
        throw new ValidateSignatureException("trans sig validate failed");
      }

      synchronized (this) {
        if (isShieldedTransaction(trx.getInstance())
            && shieldedTransInPendingCounts.get() >= shieldedTransInPendingMaxCounts) {
          return false;
        }
        if (!session.valid()) {
          session.setValue(revokingStore.buildSession());
        }

        try (ISession tmpSession = revokingStore.buildSession()) {
          processTransaction(trx, null);
          pendingTransactions.add(trx);
          tmpSession.merge();
        }
        if (isShieldedTransaction(trx.getInstance())) {
          shieldedTransInPendingCounts.incrementAndGet();
        }
      }
    } finally {
      pushTransactionQueue.remove(trx);
    }
    return true;
  }

  public void consumeMultiSignFee(TransactionCapsule trx, TransactionTrace trace)
      throws AccountResourceInsufficientException {
    if (trx.getInstance().getSignatureCount() > 1) {
      long fee = getDynamicPropertiesStore().getMultiSignFee();

      List<Contract> contracts = trx.getInstance().getRawData().getContractList();
      for (Contract contract : contracts) {
        byte[] address = TransactionCapsule.getOwner(contract);
        AccountCapsule accountCapsule = getAccountStore().get(address);
        try {
          if (accountCapsule != null) {
            adjustBalance(getAccountStore(), accountCapsule, -fee);
            adjustBalance(getAccountStore(), this.getAccountStore()
                .getBlackhole().createDbKey(), +fee);
          }
        } catch (BalanceInsufficientException e) {
          throw new AccountResourceInsufficientException(
              "Account Insufficient balance[" + fee + "] to MultiSign");
        }
      }

      trace.getReceipt().setMultiSignFee(fee);
    }
  }

  public void consumeBandwidth(TransactionCapsule trx, TransactionTrace trace)
      throws ContractValidateException, AccountResourceInsufficientException,
      TooBigTransactionResultException {
    BandwidthProcessor processor = new BandwidthProcessor(chainBaseManager);
    processor.consume(trx, trace);
  }


  /**
   * when switch fork need erase blocks on fork branch.
   */
  public synchronized void eraseBlock() {
    session.reset();
    try {
      BlockCapsule oldHeadBlock = chainBaseManager.getBlockById(
          getDynamicPropertiesStore().getLatestBlockHeaderHash());
      logger.info("start to erase block:" + oldHeadBlock);
      khaosDb.pop();
      revokingStore.fastPop();
      logger.info("end to erase block:" + oldHeadBlock);
      poppedTransactions.addAll(oldHeadBlock.getTransactions());

    } catch (ItemNotFoundException | BadItemException e) {
      logger.warn(e.getMessage(), e);
    }
  }

  public void pushVerifiedBlock(BlockCapsule block) throws ContractValidateException,
      ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      TaposException, ValidateScheduleException, ReceiptCheckErrException,
      VMIllegalException, TooBigTransactionResultException, LbeLinkedBlockException,
      NonCommonBlockException, BadNumberBlockException, BadBlockException, ZksnarkException {
    block.generatedByMyself = true;
    long start = System.currentTimeMillis();
    pushBlock(block);
    logger.info("push block cost:{}ms, blockNum:{}, blockHash:{}, trx count:{}",
        System.currentTimeMillis() - start,
        block.getNum(),
        block.getBlockId(),
        block.getTransactions().size());
  }

  private void applyBlock(BlockCapsule block) throws ContractValidateException,
      ContractExeException, ValidateSignatureException, AccountResourceInsufficientException,
      TransactionExpirationException, TooBigTransactionException, DupTransactionException,
      TaposException, ValidateScheduleException, ReceiptCheckErrException,
      VMIllegalException, TooBigTransactionResultException, ZksnarkException, BadBlockException {
    processBlock(block);
    chainBaseManager.getBlockStore().put(block.getBlockId().getBytes(), block);
    chainBaseManager.getBlockIndexStore().put(block.getBlockId());
    if (block.getTransactions().size() != 0) {
      chainBaseManager.getTransactionRetStore()
          .put(ByteArray.fromLong(block.getNum()), block.getResult());
    }

    updateFork(block);
    if (System.currentTimeMillis() - block.getTimeStamp() >= 60_000) {
      revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MAX_FLUSH_COLBET);
    } else {
      revokingStore.setMaxFlushCount(SnapshotManager.DEFAULT_MIN_FLUSH_COLBET);
    }
  }

  private void switchFork(BlockCapsule newHead)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      ValidateScheduleException, AccountResourceInsufficientException, TaposException,
      TooBigTransactionException, TooBigTransactionResultException, DupTransactionException,
      TransactionExpirationException, NonCommonBlockException, ReceiptCheckErrException,
      VMIllegalException, ZksnarkException, BadBlockException {

    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_FORK_COLBET);

    Pair<LinkedList<KhaosBlock>, LinkedList<KhaosBlock>> binaryTree;
    try {
      binaryTree =
          khaosDb.getBranch(
              newHead.getBlockId(), getDynamicPropertiesStore().getLatestBlockHeaderHash());
    } catch (NonCommonBlockException e) {
      MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_FAIL_FORK_COLBET);
      logger.info(
          "this is not the most recent common ancestor, "
              + "need to remove all blocks in the fork chain.");
      BlockCapsule tmp = newHead;
      while (tmp != null) {
        khaosDb.removeBlk(tmp.getBlockId());
        tmp = khaosDb.getBlock(tmp.getParentHash());
      }

      throw e;
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getValue())) {
      while (!getDynamicPropertiesStore()
          .getLatestBlockHeaderHash()
          .equals(binaryTree.getValue().peekLast().getParentHash())) {
        reorgContractTrigger();
        eraseBlock();
      }
    }

    if (CollectionUtils.isNotEmpty(binaryTree.getKey())) {
      List<KhaosBlock> first = new ArrayList<>(binaryTree.getKey());
      Collections.reverse(first);
      for (KhaosBlock item : first) {
        Exception exception = null;
        // todo  process the exception carefully later
        try (ISession tmpSession = revokingStore.buildSession()) {
          applyBlock(item.getBlk().setSwitch(true));
          tmpSession.commit();
        } catch (AccountResourceInsufficientException
            | ValidateSignatureException
            | ContractValidateException
            | ContractExeException
            | TaposException
            | DupTransactionException
            | TransactionExpirationException
            | ReceiptCheckErrException
            | TooBigTransactionException
            | TooBigTransactionResultException
            | ValidateScheduleException
            | VMIllegalException
            | ZksnarkException
            | BadBlockException e) {
          logger.warn(e.getMessage(), e);
          exception = e;
          throw e;
        } finally {
          if (exception != null) {
            MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_FAIL_FORK_COLBET);
            logger.warn("switch back because exception thrown while switching forks. " + exception
                    .getMessage(),
                exception);
            first.forEach(khaosBlock -> khaosDb.removeBlk(khaosBlock.getBlk().getBlockId()));
            khaosDb.setHead(binaryTree.getValue().peekFirst());

            while (!getDynamicPropertiesStore()
                .getLatestBlockHeaderHash()
                .equals(binaryTree.getValue().peekLast().getParentHash())) {
              eraseBlock();
            }

            List<KhaosBlock> second = new ArrayList<>(binaryTree.getValue());
            Collections.reverse(second);
            for (KhaosBlock khaosBlock : second) {
              // todo  process the exception carefully later
              try (ISession tmpSession = revokingStore.buildSession()) {
                applyBlock(khaosBlock.getBlk().setSwitch(true));
                tmpSession.commit();
              } catch (AccountResourceInsufficientException
                  | ValidateSignatureException
                  | ContractValidateException
                  | ContractExeException
                  | TaposException
                  | DupTransactionException
                  | TransactionExpirationException
                  | TooBigTransactionException
                  | ValidateScheduleException
                  | ZksnarkException e) {
                logger.warn(e.getMessage(), e);
              }
            }
          }
        }
      }
    }

  }

  /**
   * save a block.
   */
  public synchronized void pushBlock(final BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      LbeLinkedBlockException, ValidateScheduleException, AccountResourceInsufficientException,
      TaposException, TooBigTransactionException, TooBigTransactionResultException,
      DupTransactionException, TransactionExpirationException,
      BadNumberBlockException, BadBlockException, NonCommonBlockException,
      ReceiptCheckErrException, VMIllegalException, ZksnarkException {
    long start = System.currentTimeMillis();
    try (PendingManager pm = new PendingManager(this)) {

      if (!block.generatedByMyself) {
        if (!block.validateSignature(chainBaseManager.getDynamicPropertiesStore(),
            chainBaseManager.getAccountStore())) {
          logger.warn("The signature is not validated.");
          throw new BadBlockException("The signature is not validated");
        }

        if (!block.calcMerkleRoot().equals(block.getMerkleRoot())) {
          logger.warn(
              "The merkle root doesn't match, Calc result is "
                  + block.calcMerkleRoot()
                  + " , the headers is "
                  + block.getMerkleRoot());
          throw new BadBlockException("The merkle hash is not validated");
        }

        consensus.receiveBlock(block);
      }

      if (block.getTransactions().stream().filter(tran -> isShieldedTransaction(tran.getInstance()))
          .count() > SHIELDED_TRANS_IN_BLOCK_COLBETS) {
        throw new BadBlockException(
            "shielded transaction count > " + SHIELDED_TRANS_IN_BLOCK_COLBETS);
      }

      BlockCapsule newBlock = this.khaosDb.push(block);

      // DB don't need lower block
      if (getDynamicPropertiesStore().getLatestBlockHeaderHash() == null) {
        if (newBlock.getNum() != 0) {
          return;
        }
      } else {
        if (newBlock.getNum() <= getDynamicPropertiesStore().getLatestBlockHeaderNumber()) {
          return;
        }

        // switch fork
        if (!newBlock
            .getParentHash()
            .equals(getDynamicPropertiesStore().getLatestBlockHeaderHash())) {
          logger.warn(
              "switch fork! new head num = {}, block id = {}",
              newBlock.getNum(),
              newBlock.getBlockId());

          logger.warn(
              "******** before switchFork ******* push block: "
                  + block.toString()
                  + ", new block:"
                  + newBlock.toString()
                  + ", dynamic head num: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
                  + ", dynamic head hash: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                  + ", dynamic head timestamp: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp()
                  + ", khaosDb head: "
                  + khaosDb.getHead()
                  + ", khaosDb miniStore size: "
                  + khaosDb.getMiniStore().size()
                  + ", khaosDb unlinkMiniStore size: "
                  + khaosDb.getMiniLbelinkedStore().size());

          switchFork(newBlock);
          logger.info(SAVE_BLOCK + newBlock);

          logger.warn(
              "******** after switchFork ******* push block: "
                  + block.toString()
                  + ", new block:"
                  + newBlock.toString()
                  + ", dynamic head num: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
                  + ", dynamic head hash: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderHash()
                  + ", dynamic head timestamp: "
                  + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderTimestamp()
                  + ", khaosDb head: "
                  + khaosDb.getHead()
                  + ", khaosDb miniStore size: "
                  + khaosDb.getMiniStore().size()
                  + ", khaosDb unlinkMiniStore size: "
                  + khaosDb.getMiniLbelinkedStore().size());

          return;
        }
        try (ISession tmpSession = revokingStore.buildSession()) {

          applyBlock(newBlock);
          tmpSession.commit();
          // if event subscribe is enabled, post solidity trigger to queue
          postSolidityTrigger(getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
          // if event subscribe is enabled, post block trigger to queue
          postBlockTrigger(newBlock);
        } catch (Throwable throwable) {
          logger.error(throwable.getMessage(), throwable);
          khaosDb.removeBlk(block.getBlockId());
          throw throwable;
        }
      }
      logger.info(SAVE_BLOCK + newBlock);
    }
    //clear ownerAddressSet
    if (CollectionUtils.isNotEmpty(ownerAddressSet)) {
      Set<String> result = new HashSet<>();
      for (TransactionCapsule transactionCapsule : rePushTransactions) {
        filterOwnerAddress(transactionCapsule, result);
      }
      for (TransactionCapsule transactionCapsule : pushTransactionQueue) {
        filterOwnerAddress(transactionCapsule, result);
      }
      ownerAddressSet.clear();
      ownerAddressSet.addAll(result);
    }


    MetricsUtil.meterMark(MetricsKey.BLOCKCHAIN_BLOCK_PROCESS_TIME,
        System.currentTimeMillis() - start);

    logger.info("pushBlock block number:{}, cost/txs:{}/{}",
        block.getNum(),
        System.currentTimeMillis() - start,
        block.getTransactions().size());
  }

  public void updateDynamicProperties(BlockCapsule block) {

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderHash(block.getBlockId().getByteString());

    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderNumber(block.getNum());
    chainBaseManager.getDynamicPropertiesStore()
        .saveLatestBlockHeaderTimestamp(block.getTimeStamp());
    revokingStore.setMaxSize((int) (
        chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
            - chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum()
            + 1));
    khaosDb.setMaxSize((int)
        (chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
            - chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum()
            + 1));
  }

  /**
   * Get the fork branch.
   */
  public LinkedList<BlockId> getBlockChainHashesOnFork(final BlockId forkBlockHash)
      throws NonCommonBlockException {
    final Pair<LinkedList<KhaosBlock>, LinkedList<KhaosBlock>> branch =
        this.khaosDb.getBranch(
            getDynamicPropertiesStore().getLatestBlockHeaderHash(), forkBlockHash);

    LinkedList<KhaosBlock> blockCapsules = branch.getValue();

    if (blockCapsules.isEmpty()) {
      logger.info("empty branch {}", forkBlockHash);
      return Lists.newLinkedList();
    }

    LinkedList<BlockId> result = blockCapsules.stream()
        .map(KhaosBlock::getBlk)
        .map(BlockCapsule::getBlockId)
        .collect(Collectors.toCollection(LinkedList::new));

    result.add(blockCapsules.peekLast().getBlk().getParentBlockId());

    return result;
  }

  /**
   * Process transaction.
   */
  public TransactionInfo processTransaction(final TransactionCapsule trxCap, BlockCapsule blockCap)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, TransactionExpirationException,
      TooBigTransactionException, TooBigTransactionResultException,
      DupTransactionException, TaposException, ReceiptCheckErrException, VMIllegalException {
    if (trxCap == null) {
      return null;
    }

    validateTapos(trxCap);
    validateCommon(trxCap);

    if (trxCap.getInstance().getRawData().getContractList().size() != 1) {
      throw new ContractSizeNotEqualToOneException(
          "act size should be exactly 1, this is extend feature");
    }

    validateDup(trxCap);

    if (!trxCap.validateSignature(chainBaseManager.getAccountStore(),
        chainBaseManager.getDynamicPropertiesStore())) {
      throw new ValidateSignatureException("transaction signature validate failed");
    }

    TransactionTrace trace = new TransactionTrace(trxCap, StoreFactory.getInstance(),
        new RuntimeImpl());
    trxCap.setLbeTrace(trace);

    consumeBandwidth(trxCap, trace);
    consumeMultiSignFee(trxCap, trace);

    trace.init(blockCap, eventPluginLoaded);
    trace.checkIsConstant();
    trace.exec();

    if (Objects.nonNull(blockCap)) {
      trace.setResult();
      if (blockCap.hasWitnessSignature()) {
        if (trace.checkNeedRetry()) {
          String txId = Hex.toHexString(trxCap.getTransactionId().getBytes());
          logger.info("Retry for tx id: {}", txId);
          trace.init(blockCap, eventPluginLoaded);
          trace.checkIsConstant();
          trace.exec();
          trace.setResult();
          logger.info("Retry result for tx id: {}, tx resultCode in receipt: {}",
              txId, trace.getReceipt().getResult());
        }
        trace.check();
      }
    }

    trace.finalization();
    if (Objects.nonNull(blockCap) && getDynamicPropertiesStore().supportVM()) {
      trxCap.setResult(trace.getTransactionContext());
    }
    chainBaseManager.getTransactionStore().put(trxCap.getTransactionId().getBytes(), trxCap);

    Optional.ofNullable(transactionCache)
        .ifPresent(t -> t.put(trxCap.getTransactionId().getBytes(),
            new BytesCapsule(ByteArray.fromLong(trxCap.getBlockNum()))));

    TransactionInfoCapsule transactionInfo = TransactionUtil
        .buildTransactionInfoInstance(trxCap, blockCap, trace);

    // if event subscribe is enabled, post contract triggers to queue
    postContractTrigger(trace, false);
    Contract contract = trxCap.getInstance().getRawData().getContract(0);
    if (isMultiSignTransaction(trxCap.getInstance())) {
      ownerAddressSet.add(ByteArray.toHexString(TransactionCapsule.getOwner(contract)));
    }

    return transactionInfo.getInstance();
  }

  /**
   * Generate a block.
   */
  public synchronized BlockCapsule generateBlock(Miner miner, long blockTime, long timeout) {

    long postponedLbeCount = 0;

    BlockCapsule blockCapsule = new BlockCapsule(chainBaseManager.getHeadBlockNum() + 1,
        chainBaseManager.getHeadBlockId(),
        blockTime, miner.getWitnessAddress());
    blockCapsule.generatedByMyself = true;
    session.reset();
    session.setValue(revokingStore.buildSession());

    accountStateCallBack.preExecute(blockCapsule);

    if (getDynamicPropertiesStore().getAllowMultiSign() == 1) {
      byte[] privateKeyAddress = miner.getPrivateKeyAddress().toByteArray();
      AccountCapsule witnessAccount = getAccountStore()
          .get(miner.getWitnessAddress().toByteArray());
      if (!Arrays.equals(privateKeyAddress, witnessAccount.getWitnessPermissionAddress())) {
        logger.warn("Witness permission is wrong");
        return null;
      }
    }

    TransactionRetCapsule transactionRetCapsule = new TransactionRetCapsule(blockCapsule);

    Set<String> accountSet = new HashSet<>();
    AtomicInteger shieldedTransCounts = new AtomicInteger(0);
    Iterator<TransactionCapsule> iterator = pendingTransactions.iterator();
    while (iterator.hasNext() || rePushTransactions.size() > 0) {
      boolean fromPending = false;
      TransactionCapsule trx;
      if (iterator.hasNext()) {
        fromPending = true;
        trx = iterator.next();
      } else {
        trx = rePushTransactions.poll();
      }

      if (System.currentTimeMillis() > timeout) {
        logger.warn("Processing transaction time exceeds the producing time.");
        break;
      }

      // check the block size
      if ((blockCapsule.getInstance().getSerializedSize() + trx.getSerializedSize() + 3)
          > ChainConstant.BLOCK_SIZE) {
        postponedLbeCount++;
        continue;
      }
      //shielded transaction
      if (isShieldedTransaction(trx.getInstance())
          && shieldedTransCounts.incrementAndGet() > SHIELDED_TRANS_IN_BLOCK_COLBETS) {
        continue;
      }
      //multi sign transaction
      Contract contract = trx.getInstance().getRawData().getContract(0);
      byte[] owner = TransactionCapsule.getOwner(contract);
      String ownerAddress = ByteArray.toHexString(owner);
      if (accountSet.contains(ownerAddress)) {
        continue;
      } else {
        if (isMultiSignTransaction(trx.getInstance())) {
          accountSet.add(ownerAddress);
        }
      }
      if (ownerAddressSet.contains(ownerAddress)) {
        trx.setVerified(false);
      }
      // apply transaction
      try (ISession tmpSession = revokingStore.buildSession()) {
        accountStateCallBack.preExeTrans();
        TransactionInfo result = processTransaction(trx, blockCapsule);
        accountStateCallBack.exeTransFinish();
        tmpSession.merge();
        blockCapsule.addTransaction(trx);
        if (Objects.nonNull(result)) {
          transactionRetCapsule.addTransactionInfo(result);
        }
        if (fromPending) {
          iterator.remove();
        }
      } catch (Exception e) {
        logger.error("Process trx failed when generating block: {}", e.getMessage());
      }
    }

    accountStateCallBack.executeGenerateFinish();

    session.reset();

    logger.info("Generate block success, pendingCount: {}, rePushCount: {}, postponedCount: {}",
        pendingTransactions.size(), rePushTransactions.size(), postponedLbeCount);

    blockCapsule.setMerkleRoot();
    blockCapsule.sign(miner.getPrivateKey());

    return blockCapsule;

  }

  private void filterOwnerAddress(TransactionCapsule transactionCapsule, Set<String> result) {
    Contract contract = transactionCapsule.getInstance().getRawData().getContract(0);
    byte[] owner = TransactionCapsule.getOwner(contract);
    String ownerAddress = ByteArray.toHexString(owner);
    if (ownerAddressSet.contains(ownerAddress)) {
      result.add(ownerAddress);
    }
  }

  private boolean isMultiSignTransaction(Transaction transaction) {
    Contract contract = transaction.getRawData().getContract(0);
    switch (contract.getType()) {
      case AccountPermissionUpdateContract: {
        return true;
      }
      default:
    }
    return false;
  }

  private boolean isShieldedTransaction(Transaction transaction) {
    Contract contract = transaction.getRawData().getContract(0);
    switch (contract.getType()) {
      case ShieldedTransferContract: {
        return true;
      }
      default:
    }
    return false;
  }

  public TransactionStore getTransactionStore() {
    return chainBaseManager.getTransactionStore();
  }

  public TransactionHistoryStore getTransactionHistoryStore() {
    return chainBaseManager.getTransactionHistoryStore();
  }

  public TransactionRetStore getTransactionRetStore() {
    return chainBaseManager.getTransactionRetStore();
  }

  public BlockStore getBlockStore() {
    return chainBaseManager.getBlockStore();
  }

  /**
   * process block.
   */
  public void processBlock(BlockCapsule block)
      throws ValidateSignatureException, ContractValidateException, ContractExeException,
      AccountResourceInsufficientException, TaposException, TooBigTransactionException,
      DupTransactionException, TransactionExpirationException, ValidateScheduleException,
      ReceiptCheckErrException, VMIllegalException, TooBigTransactionResultException,
      ZksnarkException, BadBlockException {
    // todo set revoking db max size.

    // checkWitness
    if (!consensus.validBlock(block)) {
      throw new ValidateScheduleException("validateWitnessSchedule error");
    }
    //reset BlockEnergyUsage
    chainBaseManager.getDynamicPropertiesStore().saveBlockEnergyUsage(0);
    //parallel check sign
    if (!block.generatedByMyself) {
      try {
        preValidateTransactionSign(block);
      } catch (InterruptedException e) {
        logger.error("parallel check sign interrupted exception! block info: {}", block, e);
        Thread.currentThread().interrupt();
      }
    }

    TransactionRetCapsule transactionRetCapsule =
        new TransactionRetCapsule(block);
    try {
      merkleContainer.resetCurrentMerkleTree();
      accountStateCallBack.preExecute(block);
      for (TransactionCapsule transactionCapsule : block.getTransactions()) {
        transactionCapsule.setBlockNum(block.getNum());
        if (block.generatedByMyself) {
          transactionCapsule.setVerified(true);
        }
        accountStateCallBack.preExeTrans();
        TransactionInfo result = processTransaction(transactionCapsule, block);
        accountStateCallBack.exeTransFinish();
        if (Objects.nonNull(result)) {
          transactionRetCapsule.addTransactionInfo(result);
        }
      }
      accountStateCallBack.executePushFinish();
    } finally {
      accountStateCallBack.exceptionFinish();
    }
    merkleContainer.saveCurrentMerkleTreeAsBestMerkleTree(block.getNum());
    block.setResult(transactionRetCapsule);
    if (getDynamicPropertiesStore().getAllowAdaptiveEnergy() == 1) {
      EnergyProcessor energyProcessor = new EnergyProcessor(
          chainBaseManager.getDynamicPropertiesStore(), chainBaseManager.getAccountStore());
      energyProcessor.updateTotalEnergyAverageUsage();
      energyProcessor.updateAdaptiveTotalEnergyLimit();
    }

    payReward(block);

    if (chainBaseManager.getDynamicPropertiesStore().getNextMaintenanceTime()
        <= block.getTimeStamp()) {
      proposalController.processProposals();
      forkController.reset();
    }

    if (!consensus.applyBlock(block)) {
      throw new BadBlockException("consensus apply block failed");
    }

    updateTransHashCache(block);
    updateRecentBlock(block);
    updateDynamicProperties(block);
  }

  private void payReward(BlockCapsule block) {
    WitnessCapsule witnessCapsule =
        chainBaseManager.getWitnessStore().getLbechecked(block.getInstance().getBlockHeader()
            .getRawData().getWitnessAddress().toByteArray());
    if (getDynamicPropertiesStore().allowChangeDelegation()) {
      delegationService.payBlockReward(witnessCapsule.getAddress().toByteArray(),
          getDynamicPropertiesStore().getWitnessPayPerBlock());
      delegationService.payStandbyWitness();
    } else {
      byte[] witness = block.getWitnessAddress().toByteArray();
      AccountCapsule account = getAccountStore().get(witness);
      account.setAllowance(account.getAllowance()
          + chainBaseManager.getDynamicPropertiesStore().getWitnessPayPerBlock());
      getAccountStore().put(account.createDbKey(), account);
    }
  }

  private void postSolitityLogContractTrigger(Long blockNum) {
    if (Args.getSolidityContractLogTriggerList().get(blockNum) == null) {
      return;
    }
    for (ContractLogTrigger logTriggerCapsule : Args
        .getSolidityContractLogTriggerList().get(blockNum)) {
      if (chainBaseManager.getTransactionStore().getLbechecked(ByteArray.fromHexString(
          logTriggerCapsule.getTransactionId())) != null) {
        logTriggerCapsule.setTriggerName(Trigger.SOLIDITYLOG_TRIGGER_NAME);
        EventPluginLoader.getInstance().postSolidityLogTrigger(logTriggerCapsule);
      }
    }
    Args.getSolidityContractLogTriggerList().remove(blockNum);
  }

  private void postSolitityEventContractTrigger(Long blockNum) {
    if (Args.getSolidityContractEventTriggerList().get(blockNum) == null) {
      return;
    }
    for (ContractEventTrigger eventTriggerCapsule : Args
        .getSolidityContractEventTriggerList().get(blockNum)) {
      if (chainBaseManager.getTransactionStore()
          .getLbechecked(ByteArray.fromHexString(eventTriggerCapsule
          .getTransactionId())) != null) {
        eventTriggerCapsule.setTriggerName(Trigger.SOLIDITYEVENT_TRIGGER_NAME);
        EventPluginLoader.getInstance().postSolidityEventTrigger(eventTriggerCapsule);
      }
    }
    Args.getSolidityContractEventTriggerList().remove(blockNum);
  }

  private void updateTransHashCache(BlockCapsule block) {
    for (TransactionCapsule transactionCapsule : block.getTransactions()) {
      this.transactionIdCache.put(transactionCapsule.getTransactionId(), true);
    }
  }

  public void updateRecentBlock(BlockCapsule block) {
    chainBaseManager.getRecentBlockStore().put(ByteArray.subArray(
        ByteArray.fromLong(block.getNum()), 6, 8),
        new BytesCapsule(ByteArray.subArray(block.getBlockId().getBytes(), 8, 16)));
  }

  public void updateFork(BlockCapsule block) {
    forkController.update(block);
  }

  public long getSyncBeginNumber() {
    logger.info("headNumber:"
        + chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber());
    logger.info(
        "syncBeginNumber:"
            + (chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
            - revokingStore.size()));
    logger.info("solidBlockNumber:"
        + chainBaseManager.getDynamicPropertiesStore().getLatestSolidifiedBlockNum());
    return chainBaseManager.getDynamicPropertiesStore().getLatestBlockHeaderNumber()
        - revokingStore.size();
  }

  public AssetIssueStore getAssetIssueStore() {
    return chainBaseManager.getAssetIssueStore();
  }


  public AssetIssueV2Store getAssetIssueV2Store() {
    return chainBaseManager.getAssetIssueV2Store();
  }

  public AccountIdIndexStore getAccountIdIndexStore() {
    return chainBaseManager.getAccountIdIndexStore();
  }

  public NullifierStore getNullifierStore() {
    return chainBaseManager.getNullifierStore();
  }

  public void closeAllStore() {
    logger.info("******** begin to close db ********");
    chainBaseManager.closeAllStore();
    logger.info("******** end to close db ********");
  }

  public void closeOneStore(ILbeChainBase database) {
    logger.info("******** begin to close " + database.getName() + " ********");
    try {
      database.close();
    } catch (Exception e) {
      logger.info("failed to close  " + database.getName() + ". " + e);
    } finally {
      logger.info("******** end to close " + database.getName() + " ********");
    }
  }

  public boolean isTooManyPending() {
    return getPendingTransactions().size() + getRePushTransactions().size()
        > maxTransactionPendingSize;
  }

  public void preValidateTransactionSign(BlockCapsule block)
      throws InterruptedException, ValidateSignatureException {
    logger.info("PreValidate Transaction Sign, size:" + block.getTransactions().size()
        + ", block num:" + block.getNum());
    int transSize = block.getTransactions().size();
    if (transSize <= 0) {
      return;
    }
    CountDownLatch countDownLatch = new CountDownLatch(transSize);
    List<Future<Boolean>> futures = new ArrayList<>(transSize);

    for (TransactionCapsule transaction : block.getTransactions()) {
      Future<Boolean> future = validateSignService
          .submit(new ValidateSignTask(transaction, countDownLatch, chainBaseManager));
      futures.add(future);
    }
    countDownLatch.await();

    for (Future<Boolean> future : futures) {
      try {
        future.get();
      } catch (ExecutionException e) {
        throw new ValidateSignatureException(e.getCause().getMessage());
      }
    }
  }

  public void rePush(TransactionCapsule tx) {
    if (containsTransaction(tx)) {
      return;
    }

    try {
      this.pushTransaction(tx);
    } catch (ValidateSignatureException | ContractValidateException | ContractExeException
        | AccountResourceInsufficientException | VMIllegalException e) {
      logger.debug(e.getMessage(), e);
    } catch (DupTransactionException e) {
      logger.debug("pending manager: dup trans", e);
    } catch (TaposException e) {
      logger.debug("pending manager: tapos exception", e);
    } catch (TooBigTransactionException e) {
      logger.debug("too big transaction");
    } catch (TransactionExpirationException e) {
      logger.debug("expiration transaction");
    } catch (ReceiptCheckErrException e) {
      logger.debug("outOfSlotTime transaction");
    } catch (TooBigTransactionResultException e) {
      logger.debug("too big transaction result");
    }
  }

  public long getHeadBlockNum() {
    return getDynamicPropertiesStore().getLatestBlockHeaderNumber();
  }

  public void setCursor(Chainbase.Cursor cursor) {
    if (cursor == Chainbase.Cursor.PBFT) {
      long headNum = getHeadBlockNum();
      long pbftNum = chainBaseManager.getCommonDataBase().getLatestPbftBlockNum();
      revokingStore.setCursor(cursor, headNum - pbftNum);
    } else {
      revokingStore.setCursor(cursor);
    }
  }

  public void resetCursor() {
    revokingStore.setCursor(Chainbase.Cursor.HEAD, 0L);
  }

  private void startEventSubscribing() {

    try {
      eventPluginLoaded = EventPluginLoader.getInstance()
          .start(Args.getInstance().getEventPluginConfig());

      if (!eventPluginLoaded) {
        logger.error("failed to load eventPlugin");
      }

      FilterQuery eventFilter = Args.getInstance().getEventFilter();
      if (!Objects.isNull(eventFilter)) {
        EventPluginLoader.getInstance().setFilterQuery(eventFilter);
      }

    } catch (Exception e) {
      logger.error("{}", e);
    }
  }

  private void postSolidityTrigger(final long latestSolidifiedBlockNumber) {
    if (eventPluginLoaded && EventPluginLoader.getInstance().isSolidityTriggerEnable()) {
      SolidityTriggerCapsule solidityTriggerCapsule
          = new SolidityTriggerCapsule(latestSolidifiedBlockNumber);
      boolean result = triggerCapsuleQueue.offer(solidityTriggerCapsule);
      if (!result) {
        logger.info("too many trigger, lost solidified trigger, "
            + "block number: {}", latestSolidifiedBlockNumber);
      }
    }
    if (eventPluginLoaded && EventPluginLoader.getInstance().isSolidityLogTriggerEnable()) {
      postSolitityLogContractTrigger(latestSolidifiedBlockNumber);
    }
    if (eventPluginLoaded && EventPluginLoader.getInstance().isSolidityEventTriggerEnable()) {
      postSolitityEventContractTrigger(latestSolidifiedBlockNumber);
    }
  }

  private void postBlockTrigger(final BlockCapsule newBlock) {
    if (eventPluginLoaded && EventPluginLoader.getInstance().isBlockLogTriggerEnable()) {
      BlockLogTriggerCapsule blockLogTriggerCapsule = new BlockLogTriggerCapsule(newBlock);
      blockLogTriggerCapsule.setLatestSolidifiedBlockNumber(getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
      boolean result = triggerCapsuleQueue.offer(blockLogTriggerCapsule);
      if (!result) {
        logger.info("too many triggers, block trigger lost: {}", newBlock.getBlockId());
      }
    }

    for (TransactionCapsule e : newBlock.getTransactions()) {
      postTransactionTrigger(e, newBlock);
    }
  }

  private void postTransactionTrigger(final TransactionCapsule trxCap,
      final BlockCapsule blockCap) {
    if (eventPluginLoaded && EventPluginLoader.getInstance().isTransactionLogTriggerEnable()) {
      TransactionLogTriggerCapsule trx = new TransactionLogTriggerCapsule(trxCap, blockCap);
      trx.setLatestSolidifiedBlockNumber(getDynamicPropertiesStore()
          .getLatestSolidifiedBlockNum());
      boolean result = triggerCapsuleQueue.offer(trx);
      if (!result) {
        logger.info("too many triggers, transaction trigger lost: {}", trxCap.getTransactionId());
      }
    }
  }

  private void reorgContractTrigger() {
    if (eventPluginLoaded
        && (EventPluginLoader.getInstance().isContractEventTriggerEnable()
        || EventPluginLoader.getInstance().isContractLogTriggerEnable())) {
      logger.info("switchfork occurred, post reorgContractTrigger");
      try {
        BlockCapsule oldHeadBlock = chainBaseManager.getBlockById(
            getDynamicPropertiesStore().getLatestBlockHeaderHash());
        for (TransactionCapsule trx : oldHeadBlock.getTransactions()) {
          postContractTrigger(trx.getLbeTrace(), true);
        }
      } catch (BadItemException | ItemNotFoundException e) {
        logger.error("block header hash does not exist or is bad: {}",
            getDynamicPropertiesStore().getLatestBlockHeaderHash());
      }
    }
  }

  private void postContractTrigger(final TransactionTrace trace, boolean remove) {
    boolean isContractTriggerEnable = EventPluginLoader.getInstance()
        .isContractEventTriggerEnable() || EventPluginLoader
        .getInstance().isContractLogTriggerEnable();
    boolean isSolidityContractTriggerEnable = EventPluginLoader.getInstance()
        .isSolidityEventTriggerEnable() || EventPluginLoader
        .getInstance().isSolidityLogTriggerEnable();
    if (eventPluginLoaded
        && (isContractTriggerEnable || isSolidityContractTriggerEnable)) {
      // be careful, trace.getRuntimeResult().getTriggerList() should never return null
      for (ContractTrigger trigger : trace.getRuntimeResult().getTriggerList()) {
        ContractTriggerCapsule contractTriggerCapsule = new ContractTriggerCapsule(trigger);
        contractTriggerCapsule.getContractTrigger().setRemoved(remove);
        contractTriggerCapsule.setLatestSolidifiedBlockNumber(getDynamicPropertiesStore()
            .getLatestSolidifiedBlockNum());
        if (!triggerCapsuleQueue.offer(contractTriggerCapsule)) {
          logger
              .info("too many triggers, contract log trigger lost: {}", trigger.getTransactionId());
        }
      }
    }
  }

  private void prepareStoreFactory() {
    StoreFactory.init();
    StoreFactory.getInstance().setChainBaseManager(chainBaseManager);
  }

  private static class ValidateSignTask implements Callable<Boolean> {

    private TransactionCapsule trx;
    private CountDownLatch countDownLatch;
    private ChainBaseManager manager;

    ValidateSignTask(TransactionCapsule trx, CountDownLatch countDownLatch,
        ChainBaseManager manager) {
      this.trx = trx;
      this.countDownLatch = countDownLatch;
      this.manager = manager;
    }

    @Override
    public Boolean call() throws ValidateSignatureException {
      try {
        trx.validateSignature(manager.getAccountStore(), manager.getDynamicPropertiesStore());
      } catch (ValidateSignatureException e) {
        throw e;
      } finally {
        countDownLatch.countDown();
      }
      return true;
    }
  }
}
