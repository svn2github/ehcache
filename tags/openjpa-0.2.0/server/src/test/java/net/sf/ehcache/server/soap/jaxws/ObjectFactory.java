package net.sf.ehcache.server.soap.jaxws;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each
 * Java content interface and Java element interface
 * generated in the net.sf.ehcache.server.soap.jaxws package.
 * <p>An ObjectFactory allows you to programatically
 * construct new instances of the Java representation
 * for XML content. The Java representation of XML
 * content can consist of schema derived interfaces
 * and classes representing the binding of schema
 * type definitions, element declarations and model
 * groups.  Factory methods for each of these are
 * provided in this class.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _AddCache_QNAME = new QName("http://soap.server.ehcache.sf.net/", "addCache");
    private final static QName _GetResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getResponse");
    private final static QName _CacheException_QNAME = new QName("http://soap.server.ehcache.sf.net/", "CacheException");
    private final static QName _CacheNamesResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "cacheNamesResponse");
    private final static QName _GetKeysNoDuplicateCheck_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeysNoDuplicateCheck");
    private final static QName _IllegalStateException_QNAME = new QName("http://soap.server.ehcache.sf.net/", "IllegalStateException");
    private final static QName _NoSuchCacheException_QNAME = new QName("http://soap.server.ehcache.sf.net/", "NoSuchCacheException");
    private final static QName _GetQuietResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getQuietResponse");
    private final static QName _RemoveResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeResponse");
    private final static QName _GetKeysWithExpiryCheck_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeysWithExpiryCheck");
    private final static QName _Put_QNAME = new QName("http://soap.server.ehcache.sf.net/", "put");
    private final static QName _CacheNames_QNAME = new QName("http://soap.server.ehcache.sf.net/", "cacheNames");
    private final static QName _GetStatisticsAccuracyResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatisticsAccuracyResponse");
    private final static QName _PutQuietResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "putQuietResponse");
    private final static QName _GetStatisticsResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatisticsResponse");
    private final static QName _GetStatistics_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatistics");
    private final static QName _GetKeysWithExpiryCheckResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeysWithExpiryCheckResponse");
    private final static QName _PingResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "pingResponse");
    private final static QName _Load_QNAME = new QName("http://soap.server.ehcache.sf.net/", "load");
    private final static QName _GetAllWithLoaderResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getAllWithLoaderResponse");
    private final static QName _IllegalArgumentException_QNAME = new QName("http://soap.server.ehcache.sf.net/", "IllegalArgumentException");
    private final static QName _RemoveAllResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeAllResponse");
    private final static QName _GetSize_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getSize");
    private final static QName _AddCacheResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "addCacheResponse");
    private final static QName _PutResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "putResponse");
    private final static QName _GetKeysResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeysResponse");
    private final static QName _RemoveAll_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeAll");
    private final static QName _LoadAll_QNAME = new QName("http://soap.server.ehcache.sf.net/", "loadAll");
    private final static QName _ObjectExistsException_QNAME = new QName("http://soap.server.ehcache.sf.net/", "ObjectExistsException");
    private final static QName _Cache_QNAME = new QName("http://soap.server.ehcache.sf.net/", "cache");
    private final static QName _GetWithLoaderResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getWithLoaderResponse");
    private final static QName _GetStatisticsAccuracy_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatisticsAccuracy");
    private final static QName _GetSizeResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getSizeResponse");
    private final static QName _RemoveQuiet_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeQuiet");
    private final static QName _GetStatus_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatus");
    private final static QName _GetAllWithLoader_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getAllWithLoader");
    private final static QName _GetQuiet_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getQuiet");
    private final static QName _GetCacheResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getCacheResponse");
    private final static QName _RemoveQuietResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeQuietResponse");
    private final static QName _GetKeysNoDuplicateCheckResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeysNoDuplicateCheckResponse");
    private final static QName _ClearStatisticsResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "clearStatisticsResponse");
    private final static QName _GetCache_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getCache");
    private final static QName _ClearStatistics_QNAME = new QName("http://soap.server.ehcache.sf.net/", "clearStatistics");
    private final static QName _GetWithLoader_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getWithLoader");
    private final static QName _RemoveCache_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeCache");
    private final static QName _Remove_QNAME = new QName("http://soap.server.ehcache.sf.net/", "remove");
    private final static QName _Get_QNAME = new QName("http://soap.server.ehcache.sf.net/", "get");
    private final static QName _PutQuiet_QNAME = new QName("http://soap.server.ehcache.sf.net/", "putQuiet");
    private final static QName _LoadResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "loadResponse");
    private final static QName _LoadAllResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "loadAllResponse");
    private final static QName _GetStatusResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getStatusResponse");
    private final static QName _RemoveCacheResponse_QNAME = new QName("http://soap.server.ehcache.sf.net/", "removeCacheResponse");
    private final static QName _Ping_QNAME = new QName("http://soap.server.ehcache.sf.net/", "ping");
    private final static QName _GetKeys_QNAME = new QName("http://soap.server.ehcache.sf.net/", "getKeys");
    private final static QName _Element_QNAME = new QName("http://soap.server.ehcache.sf.net/", "element");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: net.sf.ehcache.server.soap.jaxws
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link ObjectExistsException }
     */
    public ObjectExistsException createObjectExistsException() {
        return new ObjectExistsException();
    }

    /**
     * Create an instance of {@link GetStatisticsAccuracy }
     */
    public GetStatisticsAccuracy createGetStatisticsAccuracy() {
        return new GetStatisticsAccuracy();
    }

    /**
     * Create an instance of {@link GetWithLoader }
     */
    public GetWithLoader createGetWithLoader() {
        return new GetWithLoader();
    }

    /**
     * Create an instance of {@link Statistics }
     */
    public Statistics createStatistics() {
        return new Statistics();
    }

    /**
     * Create an instance of {@link GetCacheResponse }
     */
    public GetCacheResponse createGetCacheResponse() {
        return new GetCacheResponse();
    }

    /**
     * Create an instance of {@link Ping }
     */
    public Ping createPing() {
        return new Ping();
    }

    /**
     * Create an instance of {@link RemoveQuiet }
     */
    public RemoveQuiet createRemoveQuiet() {
        return new RemoveQuiet();
    }

    /**
     * Create an instance of {@link GetQuiet }
     */
    public GetQuiet createGetQuiet() {
        return new GetQuiet();
    }

    /**
     * Create an instance of {@link RemoveAllResponse }
     */
    public RemoveAllResponse createRemoveAllResponse() {
        return new RemoveAllResponse();
    }

    /**
     * Create an instance of {@link RemoveCacheResponse }
     */
    public RemoveCacheResponse createRemoveCacheResponse() {
        return new RemoveCacheResponse();
    }

    /**
     * Create an instance of {@link Get }
     */
    public Get createGet() {
        return new Get();
    }

    /**
     * Create an instance of {@link GetKeysWithExpiryCheck }
     */
    public GetKeysWithExpiryCheck createGetKeysWithExpiryCheck() {
        return new GetKeysWithExpiryCheck();
    }

    /**
     * Create an instance of {@link RemoveCache }
     */
    public RemoveCache createRemoveCache() {
        return new RemoveCache();
    }

    /**
     * Create an instance of {@link AddCache }
     */
    public AddCache createAddCache() {
        return new AddCache();
    }

    /**
     * Create an instance of {@link Load }
     */
    public Load createLoad() {
        return new Load();
    }

    /**
     * Create an instance of {@link PingResponse }
     */
    public PingResponse createPingResponse() {
        return new PingResponse();
    }

    /**
     * Create an instance of {@link LoadAll }
     */
    public LoadAll createLoadAll() {
        return new LoadAll();
    }

    /**
     * Create an instance of {@link GetWithLoaderResponse }
     */
    public GetWithLoaderResponse createGetWithLoaderResponse() {
        return new GetWithLoaderResponse();
    }

    /**
     * Create an instance of {@link IllegalArgumentException }
     */
    public IllegalArgumentException createIllegalArgumentException() {
        return new IllegalArgumentException();
    }

    /**
     * Create an instance of {@link GetKeysWithExpiryCheckResponse }
     */
    public GetKeysWithExpiryCheckResponse createGetKeysWithExpiryCheckResponse() {
        return new GetKeysWithExpiryCheckResponse();
    }

    /**
     * Create an instance of {@link Put }
     */
    public Put createPut() {
        return new Put();
    }

    /**
     * Create an instance of {@link GetStatus }
     */
    public GetStatus createGetStatus() {
        return new GetStatus();
    }

    /**
     * Create an instance of {@link RemoveAll }
     */
    public RemoveAll createRemoveAll() {
        return new RemoveAll();
    }

    /**
     * Create an instance of {@link PutQuiet }
     */
    public PutQuiet createPutQuiet() {
        return new PutQuiet();
    }

    /**
     * Create an instance of {@link GetCache }
     */
    public GetCache createGetCache() {
        return new GetCache();
    }

    /**
     * Create an instance of {@link PutResponse }
     */
    public PutResponse createPutResponse() {
        return new PutResponse();
    }

    /**
     * Create an instance of {@link GetQuietResponse }
     */
    public GetQuietResponse createGetQuietResponse() {
        return new GetQuietResponse();
    }

    /**
     * Create an instance of {@link GetStatusResponse }
     */
    public GetStatusResponse createGetStatusResponse() {
        return new GetStatusResponse();
    }

    /**
     * Create an instance of {@link LoadResponse }
     */
    public LoadResponse createLoadResponse() {
        return new LoadResponse();
    }

    /**
     * Create an instance of {@link CacheException }
     */
    public CacheException createCacheException() {
        return new CacheException();
    }

    /**
     * Create an instance of {@link ClearStatisticsResponse }
     */
    public ClearStatisticsResponse createClearStatisticsResponse() {
        return new ClearStatisticsResponse();
    }

    /**
     * Create an instance of {@link Element }
     */
    public Element createElement() {
        return new Element();
    }

    /**
     * Create an instance of {@link GetAllWithLoader }
     */
    public GetAllWithLoader createGetAllWithLoader() {
        return new GetAllWithLoader();
    }

    /**
     * Create an instance of {@link GetStatistics }
     */
    public GetStatistics createGetStatistics() {
        return new GetStatistics();
    }

    /**
     * Create an instance of {@link GetSizeResponse }
     */
    public GetSizeResponse createGetSizeResponse() {
        return new GetSizeResponse();
    }

    /**
     * Create an instance of {@link GetKeysResponse }
     */
    public GetKeysResponse createGetKeysResponse() {
        return new GetKeysResponse();
    }

    /**
     * Create an instance of {@link CacheNames }
     */
    public CacheNames createCacheNames() {
        return new CacheNames();
    }

    /**
     * Create an instance of {@link RemoveQuietResponse }
     */
    public RemoveQuietResponse createRemoveQuietResponse() {
        return new RemoveQuietResponse();
    }

    /**
     * Create an instance of {@link Cache }
     */
    public Cache createCache() {
        return new Cache();
    }

    /**
     * Create an instance of {@link HashMap }
     */
    public HashMap createHashMap() {
        return new HashMap();
    }

    /**
     * Create an instance of {@link GetResponse }
     */
    public GetResponse createGetResponse() {
        return new GetResponse();
    }

    /**
     * Create an instance of {@link NoSuchCacheException }
     */
    public NoSuchCacheException createNoSuchCacheException() {
        return new NoSuchCacheException();
    }

    /**
     * Create an instance of {@link GetKeys }
     */
    public GetKeys createGetKeys() {
        return new GetKeys();
    }

    /**
     * Create an instance of {@link GetKeysNoDuplicateCheck }
     */
    public GetKeysNoDuplicateCheck createGetKeysNoDuplicateCheck() {
        return new GetKeysNoDuplicateCheck();
    }

    /**
     * Create an instance of {@link AddCacheResponse }
     */
    public AddCacheResponse createAddCacheResponse() {
        return new AddCacheResponse();
    }

    /**
     * Create an instance of {@link IllegalStateException }
     */
    public IllegalStateException createIllegalStateException() {
        return new IllegalStateException();
    }

    /**
     * Create an instance of {@link RemoveResponse }
     */
    public RemoveResponse createRemoveResponse() {
        return new RemoveResponse();
    }

    /**
     * Create an instance of {@link CacheNamesResponse }
     */
    public CacheNamesResponse createCacheNamesResponse() {
        return new CacheNamesResponse();
    }

    /**
     * Create an instance of {@link GetStatisticsResponse }
     */
    public GetStatisticsResponse createGetStatisticsResponse() {
        return new GetStatisticsResponse();
    }

    /**
     * Create an instance of {@link GetSize }
     */
    public GetSize createGetSize() {
        return new GetSize();
    }

    /**
     * Create an instance of {@link PutQuietResponse }
     */
    public PutQuietResponse createPutQuietResponse() {
        return new PutQuietResponse();
    }

    /**
     * Create an instance of {@link Remove }
     */
    public Remove createRemove() {
        return new Remove();
    }

    /**
     * Create an instance of {@link LoadAllResponse }
     */
    public LoadAllResponse createLoadAllResponse() {
        return new LoadAllResponse();
    }

    /**
     * Create an instance of {@link GetKeysNoDuplicateCheckResponse }
     */
    public GetKeysNoDuplicateCheckResponse createGetKeysNoDuplicateCheckResponse() {
        return new GetKeysNoDuplicateCheckResponse();
    }

    /**
     * Create an instance of {@link GetStatisticsAccuracyResponse }
     */
    public GetStatisticsAccuracyResponse createGetStatisticsAccuracyResponse() {
        return new GetStatisticsAccuracyResponse();
    }

    /**
     * Create an instance of {@link ClearStatistics }
     */
    public ClearStatistics createClearStatistics() {
        return new ClearStatistics();
    }

    /**
     * Create an instance of {@link GetAllWithLoaderResponse }
     */
    public GetAllWithLoaderResponse createGetAllWithLoaderResponse() {
        return new GetAllWithLoaderResponse();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddCache }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "addCache")
    public JAXBElement<AddCache> createAddCache(AddCache value) {
        return new JAXBElement<AddCache>(_AddCache_QNAME, AddCache.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getResponse")
    public JAXBElement<GetResponse> createGetResponse(GetResponse value) {
        return new JAXBElement<GetResponse>(_GetResponse_QNAME, GetResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CacheException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "CacheException")
    public JAXBElement<CacheException> createCacheException(CacheException value) {
        return new JAXBElement<CacheException>(_CacheException_QNAME, CacheException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CacheNamesResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "cacheNamesResponse")
    public JAXBElement<CacheNamesResponse> createCacheNamesResponse(CacheNamesResponse value) {
        return new JAXBElement<CacheNamesResponse>(_CacheNamesResponse_QNAME, CacheNamesResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeysNoDuplicateCheck }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeysNoDuplicateCheck")
    public JAXBElement<GetKeysNoDuplicateCheck> createGetKeysNoDuplicateCheck(GetKeysNoDuplicateCheck value) {
        return new JAXBElement<GetKeysNoDuplicateCheck>(_GetKeysNoDuplicateCheck_QNAME, GetKeysNoDuplicateCheck.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IllegalStateException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "IllegalStateException")
    public JAXBElement<IllegalStateException> createIllegalStateException(IllegalStateException value) {
        return new JAXBElement<IllegalStateException>(_IllegalStateException_QNAME, IllegalStateException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NoSuchCacheException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "NoSuchCacheException")
    public JAXBElement<NoSuchCacheException> createNoSuchCacheException(NoSuchCacheException value) {
        return new JAXBElement<NoSuchCacheException>(_NoSuchCacheException_QNAME, NoSuchCacheException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetQuietResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getQuietResponse")
    public JAXBElement<GetQuietResponse> createGetQuietResponse(GetQuietResponse value) {
        return new JAXBElement<GetQuietResponse>(_GetQuietResponse_QNAME, GetQuietResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeResponse")
    public JAXBElement<RemoveResponse> createRemoveResponse(RemoveResponse value) {
        return new JAXBElement<RemoveResponse>(_RemoveResponse_QNAME, RemoveResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeysWithExpiryCheck }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeysWithExpiryCheck")
    public JAXBElement<GetKeysWithExpiryCheck> createGetKeysWithExpiryCheck(GetKeysWithExpiryCheck value) {
        return new JAXBElement<GetKeysWithExpiryCheck>(_GetKeysWithExpiryCheck_QNAME, GetKeysWithExpiryCheck.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Put }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "put")
    public JAXBElement<Put> createPut(Put value) {
        return new JAXBElement<Put>(_Put_QNAME, Put.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CacheNames }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "cacheNames")
    public JAXBElement<CacheNames> createCacheNames(CacheNames value) {
        return new JAXBElement<CacheNames>(_CacheNames_QNAME, CacheNames.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatisticsAccuracyResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatisticsAccuracyResponse")
    public JAXBElement<GetStatisticsAccuracyResponse> createGetStatisticsAccuracyResponse(GetStatisticsAccuracyResponse value) {
        return new JAXBElement<GetStatisticsAccuracyResponse>(_GetStatisticsAccuracyResponse_QNAME, GetStatisticsAccuracyResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutQuietResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "putQuietResponse")
    public JAXBElement<PutQuietResponse> createPutQuietResponse(PutQuietResponse value) {
        return new JAXBElement<PutQuietResponse>(_PutQuietResponse_QNAME, PutQuietResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatisticsResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatisticsResponse")
    public JAXBElement<GetStatisticsResponse> createGetStatisticsResponse(GetStatisticsResponse value) {
        return new JAXBElement<GetStatisticsResponse>(_GetStatisticsResponse_QNAME, GetStatisticsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatistics }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatistics")
    public JAXBElement<GetStatistics> createGetStatistics(GetStatistics value) {
        return new JAXBElement<GetStatistics>(_GetStatistics_QNAME, GetStatistics.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeysWithExpiryCheckResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeysWithExpiryCheckResponse")
    public JAXBElement<GetKeysWithExpiryCheckResponse> createGetKeysWithExpiryCheckResponse(GetKeysWithExpiryCheckResponse value) {
        return new JAXBElement<GetKeysWithExpiryCheckResponse>(_GetKeysWithExpiryCheckResponse_QNAME, GetKeysWithExpiryCheckResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PingResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "pingResponse")
    public JAXBElement<PingResponse> createPingResponse(PingResponse value) {
        return new JAXBElement<PingResponse>(_PingResponse_QNAME, PingResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Load }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "load")
    public JAXBElement<Load> createLoad(Load value) {
        return new JAXBElement<Load>(_Load_QNAME, Load.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllWithLoaderResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getAllWithLoaderResponse")
    public JAXBElement<GetAllWithLoaderResponse> createGetAllWithLoaderResponse(GetAllWithLoaderResponse value) {
        return new JAXBElement<GetAllWithLoaderResponse>(_GetAllWithLoaderResponse_QNAME, GetAllWithLoaderResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IllegalArgumentException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "IllegalArgumentException")
    public JAXBElement<IllegalArgumentException> createIllegalArgumentException(IllegalArgumentException value) {
        return new JAXBElement<IllegalArgumentException>(_IllegalArgumentException_QNAME, IllegalArgumentException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveAllResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeAllResponse")
    public JAXBElement<RemoveAllResponse> createRemoveAllResponse(RemoveAllResponse value) {
        return new JAXBElement<RemoveAllResponse>(_RemoveAllResponse_QNAME, RemoveAllResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSize }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getSize")
    public JAXBElement<GetSize> createGetSize(GetSize value) {
        return new JAXBElement<GetSize>(_GetSize_QNAME, GetSize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AddCacheResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "addCacheResponse")
    public JAXBElement<AddCacheResponse> createAddCacheResponse(AddCacheResponse value) {
        return new JAXBElement<AddCacheResponse>(_AddCacheResponse_QNAME, AddCacheResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "putResponse")
    public JAXBElement<PutResponse> createPutResponse(PutResponse value) {
        return new JAXBElement<PutResponse>(_PutResponse_QNAME, PutResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeysResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeysResponse")
    public JAXBElement<GetKeysResponse> createGetKeysResponse(GetKeysResponse value) {
        return new JAXBElement<GetKeysResponse>(_GetKeysResponse_QNAME, GetKeysResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveAll }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeAll")
    public JAXBElement<RemoveAll> createRemoveAll(RemoveAll value) {
        return new JAXBElement<RemoveAll>(_RemoveAll_QNAME, RemoveAll.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LoadAll }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "loadAll")
    public JAXBElement<LoadAll> createLoadAll(LoadAll value) {
        return new JAXBElement<LoadAll>(_LoadAll_QNAME, LoadAll.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ObjectExistsException }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "ObjectExistsException")
    public JAXBElement<ObjectExistsException> createObjectExistsException(ObjectExistsException value) {
        return new JAXBElement<ObjectExistsException>(_ObjectExistsException_QNAME, ObjectExistsException.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Cache }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "cache")
    public JAXBElement<Cache> createCache(Cache value) {
        return new JAXBElement<Cache>(_Cache_QNAME, Cache.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetWithLoaderResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getWithLoaderResponse")
    public JAXBElement<GetWithLoaderResponse> createGetWithLoaderResponse(GetWithLoaderResponse value) {
        return new JAXBElement<GetWithLoaderResponse>(_GetWithLoaderResponse_QNAME, GetWithLoaderResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatisticsAccuracy }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatisticsAccuracy")
    public JAXBElement<GetStatisticsAccuracy> createGetStatisticsAccuracy(GetStatisticsAccuracy value) {
        return new JAXBElement<GetStatisticsAccuracy>(_GetStatisticsAccuracy_QNAME, GetStatisticsAccuracy.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSizeResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getSizeResponse")
    public JAXBElement<GetSizeResponse> createGetSizeResponse(GetSizeResponse value) {
        return new JAXBElement<GetSizeResponse>(_GetSizeResponse_QNAME, GetSizeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveQuiet }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeQuiet")
    public JAXBElement<RemoveQuiet> createRemoveQuiet(RemoveQuiet value) {
        return new JAXBElement<RemoveQuiet>(_RemoveQuiet_QNAME, RemoveQuiet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatus }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatus")
    public JAXBElement<GetStatus> createGetStatus(GetStatus value) {
        return new JAXBElement<GetStatus>(_GetStatus_QNAME, GetStatus.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllWithLoader }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getAllWithLoader")
    public JAXBElement<GetAllWithLoader> createGetAllWithLoader(GetAllWithLoader value) {
        return new JAXBElement<GetAllWithLoader>(_GetAllWithLoader_QNAME, GetAllWithLoader.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetQuiet }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getQuiet")
    public JAXBElement<GetQuiet> createGetQuiet(GetQuiet value) {
        return new JAXBElement<GetQuiet>(_GetQuiet_QNAME, GetQuiet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCacheResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getCacheResponse")
    public JAXBElement<GetCacheResponse> createGetCacheResponse(GetCacheResponse value) {
        return new JAXBElement<GetCacheResponse>(_GetCacheResponse_QNAME, GetCacheResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveQuietResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeQuietResponse")
    public JAXBElement<RemoveQuietResponse> createRemoveQuietResponse(RemoveQuietResponse value) {
        return new JAXBElement<RemoveQuietResponse>(_RemoveQuietResponse_QNAME, RemoveQuietResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeysNoDuplicateCheckResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeysNoDuplicateCheckResponse")
    public JAXBElement<GetKeysNoDuplicateCheckResponse> createGetKeysNoDuplicateCheckResponse(GetKeysNoDuplicateCheckResponse value) {
        return new JAXBElement<GetKeysNoDuplicateCheckResponse>(_GetKeysNoDuplicateCheckResponse_QNAME, GetKeysNoDuplicateCheckResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ClearStatisticsResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "clearStatisticsResponse")
    public JAXBElement<ClearStatisticsResponse> createClearStatisticsResponse(ClearStatisticsResponse value) {
        return new JAXBElement<ClearStatisticsResponse>(_ClearStatisticsResponse_QNAME, ClearStatisticsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetCache }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getCache")
    public JAXBElement<GetCache> createGetCache(GetCache value) {
        return new JAXBElement<GetCache>(_GetCache_QNAME, GetCache.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ClearStatistics }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "clearStatistics")
    public JAXBElement<ClearStatistics> createClearStatistics(ClearStatistics value) {
        return new JAXBElement<ClearStatistics>(_ClearStatistics_QNAME, ClearStatistics.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetWithLoader }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getWithLoader")
    public JAXBElement<GetWithLoader> createGetWithLoader(GetWithLoader value) {
        return new JAXBElement<GetWithLoader>(_GetWithLoader_QNAME, GetWithLoader.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveCache }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeCache")
    public JAXBElement<RemoveCache> createRemoveCache(RemoveCache value) {
        return new JAXBElement<RemoveCache>(_RemoveCache_QNAME, RemoveCache.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Remove }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "remove")
    public JAXBElement<Remove> createRemove(Remove value) {
        return new JAXBElement<Remove>(_Remove_QNAME, Remove.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Get }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "get")
    public JAXBElement<Get> createGet(Get value) {
        return new JAXBElement<Get>(_Get_QNAME, Get.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PutQuiet }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "putQuiet")
    public JAXBElement<PutQuiet> createPutQuiet(PutQuiet value) {
        return new JAXBElement<PutQuiet>(_PutQuiet_QNAME, PutQuiet.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LoadResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "loadResponse")
    public JAXBElement<LoadResponse> createLoadResponse(LoadResponse value) {
        return new JAXBElement<LoadResponse>(_LoadResponse_QNAME, LoadResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LoadAllResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "loadAllResponse")
    public JAXBElement<LoadAllResponse> createLoadAllResponse(LoadAllResponse value) {
        return new JAXBElement<LoadAllResponse>(_LoadAllResponse_QNAME, LoadAllResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetStatusResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getStatusResponse")
    public JAXBElement<GetStatusResponse> createGetStatusResponse(GetStatusResponse value) {
        return new JAXBElement<GetStatusResponse>(_GetStatusResponse_QNAME, GetStatusResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RemoveCacheResponse }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "removeCacheResponse")
    public JAXBElement<RemoveCacheResponse> createRemoveCacheResponse(RemoveCacheResponse value) {
        return new JAXBElement<RemoveCacheResponse>(_RemoveCacheResponse_QNAME, RemoveCacheResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Ping }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "ping")
    public JAXBElement<Ping> createPing(Ping value) {
        return new JAXBElement<Ping>(_Ping_QNAME, Ping.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetKeys }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "getKeys")
    public JAXBElement<GetKeys> createGetKeys(GetKeys value) {
        return new JAXBElement<GetKeys>(_GetKeys_QNAME, GetKeys.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Element }{@code >}}
     */
    @XmlElementDecl(namespace = "http://soap.server.ehcache.sf.net/", name = "element")
    public JAXBElement<Element> createElement(Element value) {
        return new JAXBElement<Element>(_Element_QNAME, Element.class, null, value);
    }

}
