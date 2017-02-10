import unittest
import click
from gm_api.app import create
from click.testing import CliRunner
from gm_api.gmapi import GMApplication, number_of_workers, cli


class TestAPIStart(unittest.TestCase):
    """Test app is ok."""

    def setUp(self):
        """Set up test fixtures."""
        self.host = '127.0.0.1'
        self.workers = 2
        self.port = 4302
        self.log = 'logs/server.log'
        options = {
            'bind': '{0}:{1}'.format(self.host, self.port),
            'workers': self.workers,
            'daemon': 'True',
            'errorlog': self.log
        }
        self.app = GMApplication(create(), options)
        # propagate the exceptions to the test client
        self.app.testing = True

    def tearDown(self):
        """Tear down test fixtures."""
        pass

    def test_command(self):
        """Test Running from command line."""
        @click.command()
        @click.option('--host')
        def start(host):
            click.echo('{0}'.format(host))

        runner = CliRunner()
        result = runner.invoke(start, input=self.host)
        assert not result.exception

    def running_app(self):
        """Test running app."""
        response = self.app.get('/')
        self.assertEqual(response.status_code, 404)

    def nb_workers(self):
        """Test running app."""
        number_of_workers()
        pass

    def start_cli(self):
        """Start cli process."""
        cli()
        pass


if __name__ == "__main__":
    unittest.main()
