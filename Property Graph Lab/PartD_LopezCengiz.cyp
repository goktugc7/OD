// Create the community
WITH ['data management', 'indexing', 'data modeling', 'big data',
	'data processing', 'data storage', 'data querying'] AS database_keywords
CREATE (c:community {name:'database'})
WITH database_keywords, c
MATCH (k:keyword)
WHERE k.name in database_keywords
CREATE (k) -[:composes]-> (c);


// Relate journals and conferences with the community
MATCH (p:paper) -[:published_in]-> (v) -[:of]-> (j)
WHERE (v:volume or v:edition) and (j:journal or j:conference)
WITH j, count(p) as total_papers
MATCH (:community {name:'database'}) <-[:composes]- (:keyword) <-[:related_to]- (p:paper) -[:published_in]-> (v) -[:of]-> (j)
WHERE (v:volume or v:edition) and (j:journal or j:conference)
WITH j, total_papers, count(distinct p) as related_papers
WHERE toFloat(related_papers) / toFloat(total_papers) >= 0.9
MATCH (c:community {name:'database'})
CREATE (j) -[:related_to]-> (c)
RETURN j;


// Highlight the top papers with page rank algorithm
CALL algo.pageRank.stream(
	'MATCH (c:community {name:"database"}) <-[:related_to]- (j) <-[:of]- (v) <-[:published_in]- (p:paper) WHERE (v:volume or v:edition) and (j:journal or j:conference) RETURN p',
	'cites', {iterations:20, dampingFactor:0.85})
YIELD nodeId, score
MATCH (p:paper) WHERE id(p) = nodeId
WITH p, score
ORDER by score DESC
WITH p, score LIMIT 100
MATCH (c:community {name:"database"})
CREATE (c) <-[:is_top_100_of]- (p)
RETURN p, score;


// Label potential reviewers and gurus for the community
MATCH (c:community {name:'database'}) <-[:is_top_100_of]- (p:paper) <-[:writes]- (a:author)
WITH c, count(p) as n_papers, a
CREATE (a) -[:potential_reviewer_of]-> (c)
WITH c, n_papers, a
WHERE n_papers >= 2
CREATE (a) -[:guru_of]-> (c);

