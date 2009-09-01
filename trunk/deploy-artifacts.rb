#!/usr/bin/env ruby

Repository = Struct.new(:id, :url)

MODULES = %w(. core debugger jcache server standalone-server jmsreplication
             jgroupsreplication openjpa)
REPOSITORIES = [
    Repository.new('sourceforge'),
    Repository.new('kong', 'file:///shares/maven2')
]

class MavenCommand
    def initialize(&block)
        instance_eval &block
    end

    attr_accessor :pom, :target, :args, :on_success, :on_failure

    def execute
        validate
        mvn_args = self.args ? self.args.join(' ') : ''
        command = "mvn -f #{pom} #{target} #{mvn_args}"
        output=`#{command}`
        if $?.success?
            on_success.call(output) if on_success
        else
            on_failure.call($?, output) if on_failure
        end
    end

    def validate
      raise("No POM specified") unless pom
      raise("No target specified") unless target
    end
end

$exit_status = 0

MODULES.each do |mod|
    module_pom = "#{mod}/pom.xml"
$stderr.puts(module_pom)
    if File.exist?(module_pom)
        clean = MavenCommand.new do
            self.pom = module_pom
            self.target = 'clean'
            self.on_failure = lambda { raise("mvn clean failed!") }
        end
        clean.execute

        REPOSITORIES.each do |repo|
            maven_deploy_command = MavenCommand.new do
                self.pom = module_pom
                self.target = 'deploy'
                self.args = ['-Dmaven.test.skip=true']
                if repo.url
                    self.args << "-DaltDeploymentRepository=#{repo.id}::default::#{repo.url}"
                end
                self.on_success = lambda { $stdout.puts("Deployed #{pom} to repository #{repo.id}") }
                self.on_failure = lambda { |status, output|
                    $exit_status = 1
                    $stderr.puts("FAILURE - failed to deploy #{pom} to repository #{repo.id}")
                    $stderr.puts(output)
                }
            end
            maven_deploy_command.execute
        end
    end
end

exit $exit_status
