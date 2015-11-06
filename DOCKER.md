Running Mineotaur in Docker
===========================

Clone this repository, and build the Mineotaur Docker image:

    docker build -t mineotaur .

Create a Mineotaur data directory, e.g. `/mineotaur-data`, containing the properties file:
- `XXX.input`

If the feature data is not provided by OMERO then also create the sample and label files:
- `XXX_sample.tsv`
- `XXX_labels.tsv`

Run the Docker image to create the Mineotaur data. The Mineotaur data directory must be mounted under `/mineotaur`, generated data will be written to a subdirectory in this directory named after the input screen:

    docker run -v /mineotaur-data:/mineotaur mineotaur -import XXX.input

If the feature data is not provided by OMERO:

    docker run -v /mineotaur-data:/mineotaur mineotaur \
        -import XXX.input XXX_sample.tsv XXX_labels.tsv

You should see several log messages, ending with

    INFO: Database generation finished. Start Mineotaur instance with -start YYY

where `YYY` is the name of the analysed screen.
Run the Mineotaur web application, substituting `YYY`:

    docker run -v /mineotaur-data:/mineotaur --start YYY

Mineotaur should be listening on port 8080 of the container.
