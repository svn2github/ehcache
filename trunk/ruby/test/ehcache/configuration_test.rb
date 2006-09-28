require 'test/unit'
require 'test/unit/assertions'
$:.push("/Users/gluck/work/ehcache/trunk/ruby-client/lib")
require 'ehcache/configuration'



class ConfigurationTest < Test::Unit::TestCase


    def test_default_constructor

        configuration = EhcacheConfiguration.new()
        puts configuration.host_name

        assert_equal(configuration.host_name, 'localhost')
        assert_equal(configuration.port, 20000)

    end

    def test_constructor

        configuration = EhcacheConfiguration.new('greg', 123)
        puts configuration.host_name

        assert_equal(configuration.host_name, 'greg')
        assert_equal(configuration.port, 123)

    end

end