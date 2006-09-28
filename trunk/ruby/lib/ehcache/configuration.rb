


# A configuration class for connection to a remote ehcache server
class EhcacheConfiguration

    attr_reader :host_name, :port 


    # The hostname the ehcache server is running on
    @host_name
    #The port the ehcache server is running on
    @port = "20000"

    #default constructor
#    def initialize(a)
#     @host_name = "localhost"
#     @port = "20000"
#    end


    #constructor with parameters
    def initialize(host_name = 'localhost', port = 20000)
     @host_name = host_name
     @port = port
    end











end
