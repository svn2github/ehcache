spec = Gem::Specification.new do |s|
  s.name              = 'ehcache-client'
  s.version           = '1.0'
  s.summary           = %q(A pure Ruby client for ehcache.)
  s.platform          = Gem::Platform::RUBY

  s.has_rdoc          = true
  s.rdoc_options      = %w(--title Ehcache::Client --main README --line-numbers)
  s.extra_rdoc_files  = %w(README ChangeLog LICENCE)

  files   = %w(README LICENCE ChangeLog bin/**/* lib/**/* demo/**/*
             images/**/* demo/**/* manual.pwd)
  s.files = FileList[*files]

  s.require_paths     = %w(lib)

  s.bindir            = %{bin}
  s.executables       = %w(techbook)

  s.author            = %q(Greg Luck)
  s.email             = %q(gregluck@users.sourceforge.net)
  s.sourceforge_project = %q(ehcache-client)
  s.homepage          = %q(http://ehcache.sf.net/documentation/ruby_client.html)
  description         = []
  File.open("README") do |file|
    file.each do |line|
      line.chomp!
      break if line.empty?
      description << "#{line.gsub(/\[\d\]/, '')}"
    end
  end
  s.description = description[1..-1].join(" ")
end
