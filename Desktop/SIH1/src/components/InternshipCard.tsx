import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { MapPin, Users, Clock, DollarSign, Building2 } from 'lucide-react';
import { toast } from 'sonner';
import { supabase } from '@/integrations/supabase/client';

interface InternshipCardProps {
  internship: {
    id: string;
    title: string;
    role: string;
    description?: string;
    required_skills: string[];
    location: string;
    sector: string;
    capacity: number;
    duration_months: number;
    stipend_amount?: number;
    company?: {
      company_name: string;
      name: string;
    };
  };
  matchScore?: number;
  matchReasons?: string[];
  showApplyButton?: boolean;
  applicationStatus?: string;
  onApply?: (internshipId: string) => void;
}

const InternshipCard = ({ 
  internship, 
  matchScore, 
  matchReasons, 
  showApplyButton = true,
  applicationStatus,
  onApply 
}: InternshipCardProps) => {
  const handleApply = async () => {
    try {
      const { data: { user } } = await supabase.auth.getUser();
      if (!user) {
        toast.error('Please login to apply');
        return;
      }

      // Check if already applied
      const { data: existingMatch } = await supabase
        .from('matches')
        .select('*')
        .eq('student_id', user.id)
        .eq('internship_id', internship.id)
        .single();

      if (existingMatch) {
        toast.info('You have already applied to this internship');
        return;
      }

      // Create application
      const { error } = await supabase
        .from('matches')
        .insert({
          student_id: user.id,
          internship_id: internship.id,
          match_score: matchScore || 75,
          status: 'applied',
          application_status: 'pending'
        });

      if (error) throw error;

      toast.success('Application submitted successfully!');
      onApply?.(internship.id);

    } catch (error: any) {
      console.error('Error applying to internship:', error);
      toast.error('Failed to submit application');
    }
  };

  const getStatusColor = (status?: string) => {
    switch (status) {
      case 'pending': return 'bg-yellow-100 text-yellow-800';
      case 'shortlisted': return 'bg-green-100 text-green-800';
      case 'rejected': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <Card className="h-full hover:shadow-lg transition-shadow">
      <CardHeader>
        <div className="flex justify-between items-start">
          <div>
            <CardTitle className="text-lg">{internship.title}</CardTitle>
            <CardDescription className="flex items-center gap-1 mt-1">
              <Building2 className="w-4 h-4" />
              {internship.company?.company_name || 'Company Name'}
            </CardDescription>
          </div>
          {matchScore && (
            <Badge variant="secondary" className="ml-2">
              {matchScore}% Match
            </Badge>
          )}
        </div>
        {applicationStatus && (
          <Badge className={getStatusColor(applicationStatus)}>
            {applicationStatus.charAt(0).toUpperCase() + applicationStatus.slice(1)}
          </Badge>
        )}
      </CardHeader>
      
      <CardContent className="space-y-4">
        <div className="flex flex-wrap gap-2">
          {internship.required_skills?.slice(0, 3).map((skill) => (
            <Badge key={skill} variant="outline" className="text-xs">
              {skill}
            </Badge>
          ))}
          {internship.required_skills?.length > 3 && (
            <Badge variant="outline" className="text-xs">
              +{internship.required_skills.length - 3} more
            </Badge>
          )}
        </div>

        {internship.description && (
          <p className="text-sm text-muted-foreground line-clamp-2">
            {internship.description}
          </p>
        )}

        <div className="space-y-2 text-sm text-muted-foreground">
          <div className="flex items-center gap-2">
            <MapPin className="w-4 h-4" />
            <span>{internship.location}</span>
          </div>
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4" />
            <span>{internship.duration_months} months</span>
          </div>
          <div className="flex items-center gap-2">
            <Users className="w-4 h-4" />
            <span>{internship.capacity} position{internship.capacity > 1 ? 's' : ''}</span>
          </div>
          {internship.stipend_amount && (
            <div className="flex items-center gap-2">
              <DollarSign className="w-4 h-4" />
              <span>₹{internship.stipend_amount.toLocaleString()}/month</span>
            </div>
          )}
        </div>

        {matchReasons && matchReasons.length > 0 && (
          <div className="space-y-1">
            <p className="text-xs font-medium text-muted-foreground">Why this matches:</p>
            <div className="flex flex-wrap gap-1">
              {matchReasons.map((reason, index) => (
                <Badge key={index} variant="secondary" className="text-xs">
                  {reason}
                </Badge>
              ))}
            </div>
          </div>
        )}

        {showApplyButton && applicationStatus !== 'pending' && applicationStatus !== 'shortlisted' && (
          <Button 
            onClick={handleApply} 
            className="w-full mt-4"
            disabled={applicationStatus === 'applied'}
          >
            {applicationStatus === 'applied' ? 'Applied' : 'Apply Now'}
          </Button>
        )}
      </CardContent>
    </Card>
  );
};

export default InternshipCard;