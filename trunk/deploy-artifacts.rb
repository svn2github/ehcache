#!/usr/bin/env ruby

DEPLOY_PROFILE = ARGV[0] || 'snapshot'

Repository = Struct.new(:id, :url)

# scan for modules
MODULES = []
Dir.chdir(File.dirname(__FILE__)) do |root|
  Dir.glob("*").each do |entry|
    if File.directory?(entry) && File.exists?(File.join(entry, "pom.xml"))
      # currently this module trigger error
      # while deploying to sonatype
      next if entry == 'console'
      MODULES << entry
    end
  end
end

REPOSITORIES = [
    Repository.new('default'),
    Repository.new('kong', 'file:///shares/maven2')
]

MODULES_TO_SONATYPE_ONLY = ['core', 'ehcache', 'terracotta']

class MavenCommand
    def initialize(&block)
        instance_eval &block
    end

    attr_accessor :pom, :target, :args, :on_success, :on_failure

    def execute
        validate
        mvn_args = self.args ? self.args.join(' ') : ''
        command = "mvn --batch-mode -f #{pom} #{target} #{mvn_args}"
        success = system("#{command}")
        if success
            on_success.call() if on_success
        else
            on_failure.call(1, "failed to publish") if on_failure
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
    puts(module_pom)
    if File.exist?(module_pom)
        clean = MavenCommand.new do
            self.pom = module_pom
            self.target = 'clean'
            self.on_failure = lambda { raise("mvn clean failed!") }
        end
        clean.execute

        REPOSITORIES.each do |repo|

            next if repo.id == 'default' && ! MODULES_TO_SONATYPE_ONLY.include?(mod)

            maven_deploy_command = MavenCommand.new do
                self.pom = module_pom
                self.target = 'deploy'
                self.args = ['-Dmaven.test.skip=true', '-Dmaven.clover.skip=true', '-Dcheckstyle.skip=true',
                             "-P '!system-tests'"]
                if DEPLOY_PROFILE == 'snapshot'
                  self.args << "-DdeploySnapshot=#{repo.id}"
                elsif DEPLOY_PROFILE == 'release'
                  self.args << "-DdeployRelease=#{repo.id}"
                end

                if repo.url
                    self.args << "-DaltDeploymentRepository=#{repo.id}::default::#{repo.url}"
                end
                self.on_success = lambda { $stdout.puts("Deployed #{pom} to repository #{repo.id}") }
                self.on_failure = lambda { |status, output|
                    $exit_status = 1
                    $stderr.puts("FAILURE - failed to deploy #{pom} to repository #{repo.id}")
                    $stderr.puts("## OUTPUT for #{pom} (#{repo.id})")
                    $stderr.puts(output)
                    $stderr.puts("## END OUTPUT for #{pom} (#{repo.id})")
                }
            end
            maven_deploy_command.execute

            # deploy EE package of standalone ehcache-terracotta
            if mod == 'terracotta' && repo.id == 'kong'
               maven_deploy_command.args << "-P package-ee"
               maven_deploy_command.execute
            end
        end
    end
end

exit $exit_status
