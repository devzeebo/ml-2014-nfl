package edu.zeebo.ml.nfl

import java.util.regex.Matcher

/**
 * User: Eric Siebeneich
 * Date: 4/7/14
 */
class CSVMerger {

	File rawDir = new File('raw')
	File cleanDir = new File('clean')

	def teamNamesToAbbr = [
			'Atlanta Falcons' : 'atl',
			'Arizona Cardinals' : 'crd',
			'Baltimore Colts' : 'clt',
			'Baltimore Ravens' : 'rav',
			'Buffalo Bills' : 'buf',
			'Carolina Panthers' : 'car',
			'Chicago Bears' : 'chi',
			'Cincinnati Bengals' : 'cin',
			'Cleveland Browns' : 'cle',
			'Dallas Cowboys' : 'dal',
			'Denver Broncos' : 'den',
			'Detroit Lions' : 'det',
			'Green Bay Packers' : 'gnb',
			'Houston Oilers' : 'oti',
			'Houston Texans' : 'htx',
			'Indianapolis Colts' : 'clt',
			'Jacksonville Jaguars' : 'jax',
			'Kansas City Chiefs' : 'kan',
			'Los Angeles Raiders' : 'rai',
			'Los Angeles Rams' : 'ram',
			'Miami Dolphins' : 'mia',
			'Minnesota Vikings' : 'min',
			'New England Patriots' : 'nwe',
			'New Orleans Saints' : 'nor',
			'New York Giants' : 'nyg',
			'New York Jets' : 'nyj',
			'Oakland Raiders' : 'rai',
			'Philadelphia Eagles' : 'phi',
			'Phoenix Cardinals' : 'crd',
			'Pittsburgh Steelers' : 'pit',
			'San Diego Chargers' : 'sdg',
			'San Francisco 49ers' : 'sfo',
			'Seattle Seahawks' : 'sea',
			'St. Louis Cardinals' : 'crd',
			'St. Louis Rams' : 'ram',
			'Tampa Bay Buccaneers' : 'tam',
			'Tennessee Oilers' : 'oti',
			'Tennessee Titans' : 'oti',
			'Washington Redskins' : 'was'
	]

	def nflMetaData = [:]

	// year -> teamabbv -> week : [w, l, t]
	def records = [:]

	CSVMerger() {

		cleanDir.mkdirs()

		cleanMetaData()

		(1980..2013).each {
			cleanGameData(it)
		}

		calculateRecords()

		mergeMetaAndGameData()
	}

	def cleanMetaData() {

		new File(rawDir, 'uncleaned_nfl_game_metadata_since_1980.csv').withReader { reader ->

			List<String> rawHeaders = reader.readLine().split(',')
			def lines = reader.readLines()

			lines.each { it ->
				// Fancy (cheap) CSV parser regex
				Matcher m = it =~ /(?:"([^"]*)")|(?:(?<=,|^)([^,]*)(?=,|$))/
				it = []
				while(m.find()) {
					it << m.group().replace('"', '')
				}

				// Check if its within our data range
				if ((it[2][(0..3)] as int) >= 1980) {
					if (!nflMetaData[it[2]]) {
						nflMetaData[it[2]] = [:]
					}

					if (it[0] == 'Weather') {
						it[1].split(',').each { weather ->
							if (weather.contains('degrees')) {
								nflMetaData[it[2]]['temperature'] = weather.replace('degrees', '').trim()
							}
							if (weather.contains('mph')) {
								nflMetaData[it[2]]['wind'] = weather.replace('wind', '').replace(' mph', '').trim()
							}
							if (weather.contains('humidity')) {
								nflMetaData[it[2]]['humidity'] = weather.replace('relative humidity', '').trim()
							}
							if (weather.contains('chill')) {
								nflMetaData[it[2]]['wind chill'] = weather.replace('wind chill', '').trim()
							}
						}
					}
					else {
						nflMetaData[it[2]][it[0].toLowerCase()] = it[1].replace(',', '')
					}
				}
			}
		}
	}

	def cleanGameData(def year) {

		File rawFile = new File(rawDir, "${year}.csv")
		File cleanFile = new File(cleanDir, "${year}.csv")

		def headers = ['week', 'date', 'game type', 'home', 'away', 'winner', 'loser', 'tie']

		rawFile.withReader { reader ->

			List<String> rawHeaders = reader.readLine().split(',')
			List<String> lines = reader.readLines()

			cleanFile.withWriter { writer ->

				def gameType = 'season'
				writer.println headers.join(',')

				lines.each { it ->

					it = it.split(',')
					if (it[2] == 'Playoffs') {
						gameType = 'playoff'
					}
					else if (it[0] != 'Week') {

						def outLine = []
						def gameDate = Date.parse('MMMMM d yyyy', "${it[rawHeaders.indexOf('Date')]} $year")

						String week = it[rawHeaders.indexOf('Week')]
						outLine << week
						outLine << gameDate.format('E d MMM yyyy')

						if (gameType == 'playoff') {
							outLine << it[rawHeaders.indexOf('Week')]
							outLine[0] = '*'
						}
						else {
							outLine << gameType
						}

						String winner = it[rawHeaders.indexOf('Winner/tie')]
						String loser = it[rawHeaders.indexOf('Loser/tie')]
						String home, away

						if (it[rawHeaders.lastIndexOf('')] == '@') {
							(home, away) = [loser, winner]
						}
						else {
							(home, away) = [winner, loser]
						}

						outLine << home
						outLine << away
						outLine << winner
						outLine << loser

						boolean tie = it[rawHeaders.indexOf('PtsW')] == it[rawHeaders.indexOf('PtsL')]
						outLine << tie

						String key = gameDate.format('yyyyMMdd') + '0' + teamNamesToAbbr[home]

						if (!nflMetaData[key]) {
							nflMetaData[key] = [:]
						}

						nflMetaData[key]['home'] = home
						nflMetaData[key]['away'] = away
						nflMetaData[key]['winner'] = winner
						nflMetaData[key]['loser'] = loser
						nflMetaData[key]['week'] = week
						nflMetaData[key]['tie'] = tie

						writer.println outLine.join(',')
					}
				}
			}
		}
	}

	def calculateRecords() {
		nflMetaData.keySet().sort().each { String key ->
			String year = key[0..<4]
			if (!records[year]) {
				records[year] = [:]
			}
			String winnerAbbr = teamNamesToAbbr[nflMetaData[key].winner]
			String loserAbbr = teamNamesToAbbr[nflMetaData[key].loser]

			if (!records[year][winnerAbbr]) {
				records[year][winnerAbbr] = [0 : [0, 0, 0]]
			}
			if (!records[year][loserAbbr]) {
				records[year][loserAbbr] = [0 : [0, 0, 0]]
			}

			def week = nflMetaData[key].week

			if (week && week.length() < 3) {
				week = week as int

				println year + " " + week + " " + key + " " + records[year][winnerAbbr].keySet().max()
				println year + " " + week + " " + key + " " + records[year][loserAbbr].keySet().max()

				def winnerRecord = records[year][winnerAbbr][records[year][winnerAbbr].keySet().max()]
				def loserRecord = records[year][loserAbbr][records[year][loserAbbr].keySet().max()]

				nflMetaData[key].'winner record' = winnerRecord
				nflMetaData[key].'loser record' = loserRecord

				winnerRecord = [winnerRecord].flatten()
				loserRecord = [loserRecord].flatten()

				if (nflMetaData[key].tie) {
					winnerRecord[2]++
					loserRecord[2]++
				}
				else {
					winnerRecord[0]++
					loserRecord[1]++
				}
				records[year][winnerAbbr][week] = winnerRecord
				records[year][loserAbbr][week] = loserRecord
			}
		}
	}

	def mergeMetaAndGameData() {
		def headers = ['stadium', 'date', 'surface', 'temperature', 'humidity', 'wind', 'wind chill', 'vegas line', 'attendance',
				'week', 'home', 'away', 'winner', 'winner record', 'loser', 'loser record', 'tie']

		new File(cleanDir, 'nfl_game_data.csv').withWriter { writer ->

			writer.println(['id', headers*.toLowerCase()].flatten().join(','))

			nflMetaData.keySet().sort().each {
				def line = [it]

				it = nflMetaData[it]

				headers.each { col ->
					line << (it[col] ?: '')
				}

				writer.println line.join(',')
			}
		}
	}

	static def main(args) {
		new CSVMerger()
	}
}
